/*********************************************************************************************
 *
 * 'GamlHoverProvider.java, in plugin ummisco.gama.ui.modeling, is part of the source code of the GAMA modeling and
 * simulation platform. (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package msi.gama.lang.gaml.ui.hover;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.EObjectAtOffsetHelper;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.hover.DispatchingEObjectTextHover;
import org.eclipse.xtext.ui.editor.hover.html.DefaultEObjectHoverProvider;
import org.eclipse.xtext.ui.editor.hover.html.IXtextBrowserInformationControl;
import org.eclipse.xtext.ui.editor.hover.html.XtextBrowserInformationControl;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.util.Pair;
import org.eclipse.xtext.util.Tuples;

import com.google.inject.Inject;

import msi.gama.common.interfaces.IGamlDescription;
import msi.gama.lang.gaml.EGaml;
import msi.gama.lang.gaml.gaml.ActionDefinition;
import msi.gama.lang.gaml.gaml.ActionRef;
import msi.gama.lang.gaml.gaml.Facet;
import msi.gama.lang.gaml.gaml.Function;
import msi.gama.lang.gaml.gaml.Import;
import msi.gama.lang.gaml.gaml.S_Definition;
import msi.gama.lang.gaml.gaml.S_Do;
import msi.gama.lang.gaml.gaml.S_Global;
import msi.gama.lang.gaml.gaml.Statement;
import msi.gama.lang.gaml.gaml.TypeRef;
import msi.gama.lang.gaml.gaml.UnitFakeDefinition;
import msi.gama.lang.gaml.gaml.UnitName;
import msi.gama.lang.gaml.gaml.VarDefinition;
import msi.gama.lang.gaml.gaml.VariableRef;
import msi.gama.lang.gaml.resource.GamlResourceServices;
import msi.gaml.descriptions.FacetProto;
import msi.gaml.descriptions.SymbolProto;
import msi.gaml.expressions.UnitConstantExpression;
import msi.gaml.factories.DescriptionFactory;
import msi.gaml.operators.IUnits;
import ummisco.gama.ui.utils.WorkbenchHelper;

public class GamlHoverProvider extends DefaultEObjectHoverProvider {

	public static class NonXRefEObjectAtOffset extends EObjectAtOffsetHelper {

		@Override
		protected EObject resolveCrossReferencedElement(final INode node) {
			final EObject referenceOwner = NodeModelUtils.findActualSemanticObjectFor(node);
			return referenceOwner;
		}

	}

	public static class GamlDispatchingEObjectTextHover extends DispatchingEObjectTextHover {

		@Inject private EObjectAtOffsetHelper eObjectAtOffsetHelper;

		@Inject private ILocationInFileProvider locationInFileProvider;

		EObject correct = null;

		@Override
		protected Pair<EObject, IRegion> getXtextElementAt(final XtextResource resource, final int offset) {
			// BUGFIX AD 2/4/13 : getXtextElementAt() is called twice, one to
			// compute the region
			// from the UI thread, one to compute the objects from the hover
			// thread. The offset in
			// the second call is always false (maybe we should file a bug in
			// XText). The following
			// code is a workaround.
			ITextRegion region = null;
			EObject o;
			if (correct == null) {
				correct = eObjectAtOffsetHelper.resolveContainedElementAt(resource, offset);
				o = correct;
			} else {
				o = correct;
				correct = null;
			}
			// /BUGFIX
			if (o != null) {
				// scope.getGui().debug("Object under hover:" + o.toString());
				if (o instanceof ActionRef) {
					final EObject container = o.eContainer();
					// scope.getGui().debug("Found " + ((ActionRef)
					// o).getRef().getName());
					if (container instanceof Function) {
						// scope.getGui().debug("---> Is a function");
						o = container;
						region = locationInFileProvider.getFullTextRegion(o);
					}
				}
				if (region == null) {
					region = locationInFileProvider.getSignificantTextRegion(o);
				}
				final IRegion region2 = new Region(region.getOffset(), region.getLength());
				/*
				 * if ( TextUtilities.overlaps(region2, new Region(offset, 0)) )
				 */ {
					return Tuples.create(o, region2);
				}
			} else {
				final ILeafNode node =
						NodeModelUtils.findLeafNodeAtOffset(resource.getParseResult().getRootNode(), offset);
				if (node != null && node.getGrammarElement() instanceof Keyword) {
					final IRegion region2 = new Region(node.getOffset(), node.getLength());
					return Tuples.create(node.getGrammarElement(), region2);
				}
			}
			return null;
		}

		@Override
		public Object getHoverInfo(final EObject first, final ITextViewer textViewer, final IRegion hoverRegion) {
			return super.getHoverInfo(first, textViewer, hoverRegion);
		}

	}

	public class GamlHoverControlCreator extends HoverControlCreator {

		/**
		 * @param informationPresenterControlCreator
		 */
		public GamlHoverControlCreator(final IInformationControlCreator informationPresenterControlCreator) {
			super(informationPresenterControlCreator);
		}

		public class GamlInformationControl extends XtextBrowserInformationControl {

			@Override
			public void setSize(final int width, final int height) {
				super.setSize(width, height);
				final org.eclipse.swt.graphics.Point p = WorkbenchHelper.getDisplay().getCursorLocation();
				p.x -= 5;
				p.y += 15;
				setLocation(p);
			}

			/**
			 * @param parent
			 * @param symbolicFontName
			 * @param statusFieldText
			 */
			public GamlInformationControl(final Shell parent, final String symbolicFontName,
					final String statusFieldText) {
				super(parent, symbolicFontName, statusFieldText);
			}

			/*
			 * @see org.eclipse.jface.text.IInformationControlExtension5# getInformationPresenterControlCreator()
			 */
			@Override
			public IInformationControlCreator getInformationPresenterControlCreator() {
				return GamlHoverProvider.this.getInformationPresenterControlCreator();
			}
		}

		@Override
		public IInformationControl doCreateInformationControl(final Shell parent) {

			final String tooltipAffordanceString = EditorsUI.getTooltipAffordanceString();
			if (BrowserInformationControl.isAvailable(parent)) {
				final String font = "org.eclipse.jdt.ui.javadocfont"; // FIXME:
																		// PreferenceConstants.APPEARANCE_JAVADOC_FONT;
				final IXtextBrowserInformationControl iControl =
						new GamlInformationControl(parent, font, tooltipAffordanceString) {

						};
				addLinkListener(iControl);
				return iControl;
			} else {
				return new DefaultInformationControl(parent, tooltipAffordanceString);
			}
		}
	}

	private IInformationControlCreator creator;

	@Override
	public IInformationControlCreator getHoverControlCreator() {
		if (creator == null) {
			creator = new GamlHoverControlCreator(getInformationPresenterControlCreator());
		}
		return creator;
	}

	@Override
	public IInformationControlCreatorProvider getHoverInfo(final EObject first, final ITextViewer textViewer,
			final IRegion hoverRegion) {

		return super.getHoverInfo(first, textViewer, hoverRegion);

	}

	//
	// @Override
	// protected String getHoverInfoAsHtml(final EObject o) {
	// String s = super.getHoverInfoAsHtml(o);
	// if ( s == null || s.trim().isEmpty() ) { return null; }
	// return s;
	// }

	@Override
	protected boolean hasHover(final EObject o) {
		return true;
		// String s = getFirstLine(o);
		// return s != null && !s.isEmpty();
	}

	@Override
	protected String getFirstLine(final EObject o) {
		if (o instanceof Import) {
			String uri = ((Import) o).getImportURI();
			uri = uri.substring(uri.lastIndexOf('/') + 1);
			final String model = ((Import) o).getName() != null ? "micro-model" : "model";
			return "Import of the " + model + " defined in <b>" + uri + "</b>";
		}
		if (o instanceof S_Global) { return "Global definitions of " + getFirstLine(o.eContainer().eContainer()); }
		final Statement s = EGaml.getInstance().getStatement(o);
		if (o instanceof TypeRef && s instanceof S_Definition && ((S_Definition) s).getTkey() == o) {
			return getFirstLine(s);
		}
		// Case of do xxx;
		if (o instanceof VariableRef && o.eContainer() instanceof S_Do && ((S_Do) o.eContainer()).getExpr() == o) {
			VarDefinition vd = ((VariableRef) o).getRef();
			final IGamlDescription description = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(vd);
			if (description != null) {
				String result = description.getTitle();
				if (result == null || result.isEmpty()) { return ""; }
				result = "<b>" + result + "</b>";
				return result;
			}
		}
		if (o instanceof Function) {
			final ActionRef ref = getActionFrom((Function) o);
			if (ref != null) {
				final ActionDefinition def = ref.getRef();
				if (def != null) {
					final String temp = getFirstLine(def);
					if (!temp.isEmpty()) { return temp; }
				}
			}
		} else if (o instanceof UnitName) {
			final UnitFakeDefinition fake = ((UnitName) o).getRef();
			if (fake == null) { return "<b> Unknown unit or constant </b>"; }
			final UnitConstantExpression unit = IUnits.UNITS_EXPR.get(fake.getName());
			if (unit == null) { return "<b> Unknown unit or constant </b>"; }
			return "<b>" + unit.getTitle() + "</b>";
		}

		final IGamlDescription description = GamlResourceServices.getResourceDocumenter().getGamlDocumentation(o);
		if (description == null) {
			if (o instanceof Facet) { return "<b>" + getFirstLineOf((Facet) o) + "</b>"; }

			if (s != null && DescriptionFactory.isStatementProto(EGaml.getInstance().getKeyOf(o))) {
				if (s == o) { return ""; }
				return getFirstLine(s);
			} else {
				if (o instanceof TypeRef) {
					return "Type " + EGaml.getInstance().getKeyOf(o);
				} else {
					return "";
				}
			}
		} else {
			String result = description.getTitle();
			if (result == null || result.isEmpty()) { return ""; }
			result = "<b>" + result + "</b>";
			return result;
		}
	}

	private ActionRef getActionFrom(final Function f) {
		if (f.getLeft() instanceof ActionRef) { return (ActionRef) f.getLeft(); }
		return null;
	}

	/**
	 * @param o
	 * @return
	 */
	private String getFirstLineOf(final Facet o) {

		String facetName = o.getKey();
		facetName = facetName.substring(0, facetName.length() - 1);
		final EObject cont = o.eContainer();
		final String key = EGaml.getInstance().getKeyOf(cont);
		final SymbolProto p = DescriptionFactory.getProto(key, null);
		if (p != null) {
			final FacetProto f = p.getPossibleFacets().get(facetName);
			if (f != null) { return f.getTitle(); }
		}
		return "Facet " + o.getKey();

	}
}