package gama.processor.engine;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import gama.processor.annotations.GamlAnnotations.arg;
import gama.processor.annotations.GamlAnnotations.constant;
import gama.processor.annotations.GamlAnnotations.display;
import gama.processor.annotations.GamlAnnotations.doc;
import gama.processor.annotations.GamlAnnotations.example;
import gama.processor.annotations.GamlAnnotations.experiment;
import gama.processor.annotations.GamlAnnotations.facet;
import gama.processor.annotations.GamlAnnotations.file;
import gama.processor.annotations.GamlAnnotations.no_test;
import gama.processor.annotations.GamlAnnotations.operator;
import gama.processor.annotations.GamlAnnotations.skill;
import gama.processor.annotations.GamlAnnotations.species;
import gama.processor.annotations.GamlAnnotations.symbol;
import gama.processor.annotations.GamlAnnotations.test;
import gama.processor.annotations.GamlAnnotations.tests;
import gama.processor.annotations.GamlAnnotations.type;
import gama.processor.annotations.GamlAnnotations.usage;
import gama.processor.annotations.GamlAnnotations.variable;

public abstract class ElementProcessor<T extends Annotation> implements IProcessor<T>, Constants {

	protected static final Map<String, String> NAME_CACHE = new HashMap<>();

	protected final Map<String, StringBuilder> opIndex = new HashMap<>();
	static final Pattern CLASS_PARAM = Pattern.compile("<.*?>");
	static final Pattern SINGLE_QUOTE = Pattern.compile("\"");
	static final String QUOTE_MATCHER = Matcher.quoteReplacement("\\\"");
	public static final Map<String, String> javaClassNamesToGamlTypes = new HashMap<>();
	public static final Map<Integer, String> typeIndicesToGamlTypes = new HashMap<>();
	protected String initializationMethodName;

	public ElementProcessor() {}

	protected void clean(final ProcessorContext context, final Map<String, StringBuilder> map) {
		for (final String k : context.getRoots()) {
			map.remove(k);
		}
	}

	@Override
	public boolean hasElements() {
		return opIndex.size() > 0;
	}

	@Override
	public void process(final ProcessorContext context) {
		final Class<T> a = getAnnotationClass();
		clean(context, opIndex);
		for (final Map.Entry<String, List<Element>> entry : context.groupElements(a).entrySet()) {
			final List<Element> elements = entry.getValue();
			if (elements.size() == 0) {
				continue;
			}
			final StringBuilder sb = new StringBuilder();
			for (final Element e : elements) {
				try {
					createElement(sb, context, e, e.getAnnotation(a));
				} catch (final Exception exception) {

					context.emitError("Exception in processor: " + exception.getMessage(), exception, e);

				}

			}
			if (sb.length() > 0) {
				opIndex.put(entry.getKey(), sb);
			}
		}
	}

	static final doc[] NULL_DOCS = new doc[0];

	protected boolean isInternal(final Element main, final Annotation a) {
		boolean internal = false;
		if (a instanceof species) {
			internal = ((species) a).internal();
		} else if (a instanceof symbol) {
			internal = ((symbol) a).internal();
		} else if (a instanceof operator) {
			internal = ((operator) a).internal();
		} else if (a instanceof skill) {
			internal = ((skill) a).internal();
		} else if (a instanceof facet) {
			internal = ((facet) a).internal();
		} else if (a instanceof type) {
			internal = ((type) a).internal();
		} else if (a instanceof variable) {
			internal = ((variable) a).internal();
		}
		return internal;
	}

	protected doc getDocAnnotation(final Element main, final Annotation a) {
		doc[] docs = NULL_DOCS;
		if (a instanceof species) {
			docs = ((species) a).doc();
		} else if (a instanceof symbol) {
			docs = ((symbol) a).doc();
		} else if (a instanceof arg) {
			docs = ((arg) a).doc();
		} else if (a instanceof display) {
			// nothing
		} else if (a instanceof experiment) {
			// nothing
		} else if (a instanceof constant) {
			docs = ((constant) a).doc();
		} else if (a instanceof operator) {
			docs = ((operator) a).doc();
		} else if (a instanceof skill) {
			docs = ((skill) a).doc();
		} else if (a instanceof facet) {
			docs = ((facet) a).doc();
		} else if (a instanceof type) {
			docs = ((type) a).doc();
		} else if (a instanceof file) {
			docs = ((file) a).doc();
		} else if (a instanceof variable) {
			docs = ((variable) a).doc();
		}
		doc d = null;
		if (docs.length == 0) {
			d = main.getAnnotation(doc.class);
		} else {
			d = docs[0];
		}
		return d;
	}

	protected boolean isDeprecated(final Element e, final Annotation a) {
		final doc d = getDocAnnotation(e, a);
		if (d == null)
			return false;
		return d.deprecated().length() > 0;
	}

	public boolean hasTests(final example[] examples) {
		for (final example ex : examples) {
			if (ex.isTestOnly() || ex.isExecutable() && ex.test())
				return true;
		}
		return false;
	}

	public boolean hasTests(final Element e, final Annotation a) {
		// if the artifact is internal, skip the verification
		if (isInternal(e, a))
			return true;
		final no_test no = e.getAnnotation(no_test.class);
		// if no tests are necessary, skip the verification
		if (no != null)
			return true;
		final tests tests = e.getAnnotation(tests.class);
		if (tests != null)
			return true;
		final test test = e.getAnnotation(test.class);
		if (test != null)
			return true;
		final doc doc = getDocAnnotation(e, a);
		if (doc == null)
			return false;
		if (hasTests(doc.examples()))
			return true;
		for (final usage us : doc.usages()) {
			if (hasTests(us.examples()))
				return true;
		}
		return doc.deprecated().length() > 0;
	}

	protected void verifyDoc(final ProcessorContext context, final Element e, final String displayedName,
			final Annotation a) {
		if (isInternal(e, a))
			return;
		final doc d = getDocAnnotation(e, a);
		boolean docMissing = d == null;
		if (d != null) {
			if (d.value().length() == 0 && d.deprecated().length() == 0 && d.usages().length == 0
					&& d.special_cases().length == 0 && d.examples().length == 0) {
				docMissing = true;
			}
		}
		if (docMissing) {
			context.emitWarning("GAML: documentation missing for " + displayedName, e);
		}

	}

	@Override
	public void serialize(final ProcessorContext context, final StringBuilder sb) {
		opIndex.forEach((s, builder) -> {
			if (builder != null) {
				sb.append(builder);
			}
		});
	}

	public abstract void createElement(StringBuilder sb, ProcessorContext context, Element e, T annotation);

	protected abstract Class<T> getAnnotationClass();

	@Override
	public final String getInitializationMethodName() {
		if (initializationMethodName == null) {
			initializationMethodName =
					"initialize" + Constants.capitalizeFirstLetter(getAnnotationClass().getSimpleName());
		}
		return initializationMethodName;
	}

	protected static String toJavaString(final String s) {
		if (s == null || s.isEmpty())
			return "(String)null";
		final int i = ss1.indexOf(s);
		return i == -1 ? "\"" + s + "\"" : ss2.get(i);
	}

	protected final static String toClassObject(final String s) {
		final String result = CLASS_NAMES.get(s);
		return result == null ? s + ".class" : result;
	}

	protected final static StringBuilder toArrayOfStrings(final String[] segments, final StringBuilder sb) {
		if (segments == null || segments.length == 0) {
			sb.append("AS");
			return sb;
		}
		sb.append("S(");
		for (int i = 0; i < segments.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(toJavaString(segments[i]));
		}
		sb.append(')');
		return sb;
	}

	protected static String checkPrim(final String c) {
		final String result = CHECK_PRIM.get(c);
		return result == null ? c : result;
	}

	protected static String returnWhenNull(final String returnClass) {
		final String result = RETURN_WHEN_NULL.get(returnClass);
		return result == null ? " null" : result;
	}

	protected static void param(final StringBuilder sb, final String c, final String par) {
		final String jc = checkPrim(c);
		switch (jc) {
			case DOUBLE:
				sb.append("asFloat(s,").append(par).append(')');
				break;
			case INTEGER:
				sb.append("asInt(s,").append(par).append(')');
				break;
			case BOOLEAN:
				sb.append("asBool(s,").append(par).append(')');
				break;
			case OBJECT:
				sb.append(par);
				break;
			default:
				sb.append("((").append(jc).append(")").append(par).append(')');

		}
	}

	protected static String escapeDoubleQuotes(final String input) {
		if (input == null)
			return "";
		return SINGLE_QUOTE.matcher(input).replaceAll(QUOTE_MATCHER);
	}

	public static StringBuilder toArrayOfInts(final int[] array, final StringBuilder sb) {
		if (array == null || array.length == 0) {
			sb.append("AI");
			return sb;
		}
		sb.append("I(");
		for (final int i : array) {
			sb.append(i).append(",");
		}
		sb.setLength(sb.length() - 1);
		sb.append(")");
		return sb;
	}

	static String rawNameOf(final ProcessorContext context, final TypeMirror t) {
		if (t.getKind().equals(TypeKind.VOID))
			return "void";
		final String key = t.toString();
		if (NAME_CACHE.containsKey(key))
			return NAME_CACHE.get(key);
		String type = context.getTypeUtils().erasure(t).toString();
		// As a workaround for ECJ/javac discrepancies regarding erasure
		type = CLASS_PARAM.matcher(type).replaceAll("");
		// Reduction by considering the imports written in the header
		for (final String element : GamaProcessor.IMPORTS) {
			if (type.startsWith(element)) {
				type = type.replace(element + ".", "");
				break;
			}
		}
		// context.emit(Kind.NOTE, "Type to convert : " + key + " | Reduction: " + type, null);
		NAME_CACHE.put(key, type);
		return type;
	}

	protected String toBoolean(final boolean b) {
		return b ? "T" : "F";
	}

	public static void addType(final String symbol, final int index, final Object... classes) {
		typeIndicesToGamlTypes.put(index, symbol);
		for (Object className : classes) {
			javaClassNamesToGamlTypes.put(className.toString(), symbol);
		}
	}

	public static String getProperType(final String rawName) {

		// Get only the first <
		final String[] splitByLeftBracket = rawName.split("<", 2);

		// Stop criteria: no bracket
		if (splitByLeftBracket.length == 1) {
			String type = splitByLeftBracket[0];
			if (javaClassNamesToGamlTypes.containsKey(type))
				return javaClassNamesToGamlTypes.get(type);
			else {
				for (String key : javaClassNamesToGamlTypes.keySet()) {
					if (key.endsWith(rawName) || rawName.endsWith(key))
						return javaClassNamesToGamlTypes.get(key);
				}
			}

			return "unknown";
		} else if (splitByLeftBracket.length == 2) {
			final String leftElement = getProperType(splitByLeftBracket[0]);

			final String lastString = splitByLeftBracket[1];
			splitByLeftBracket[1] = lastString.substring(0, lastString.length() - 1);

			// Get only the first ","
			final int comaIndex = findCentralComa(splitByLeftBracket[1]);
			if (comaIndex > 0)
				return leftElement + "<" + getProperType(splitByLeftBracket[1].substring(0, comaIndex)) + ","
						+ getProperType(splitByLeftBracket[1].substring(comaIndex + 1)) + ">";
			else
				return leftElement + "<" + getProperType(splitByLeftBracket[1]) + ">";
		} else
			throw new IllegalArgumentException("getProperType has a not appropriate input");

	}

	public static int findCentralComa(final String s) {
		int foundIndex = 0;

		if (s.contains(",")) {
			foundIndex = s.indexOf(",", 0);

			do {
				final String sLeft = s.substring(0, foundIndex);

				if (sLeft.lastIndexOf("<") == -1 && sLeft.lastIndexOf(">") == -1)
					return foundIndex;
				else if (sLeft.lastIndexOf(">") > sLeft.lastIndexOf("<"))
					return foundIndex;

				foundIndex = s.indexOf(",", foundIndex + 1);

			} while (foundIndex >= 0);
			return -1;
		}
		return -1;
	}

	public static String getTypeString(final int type) {
		String result = typeIndicesToGamlTypes.get(type);
		if (result == null)
			return "unknown";
		return result;
	}

	public static String getTypeString(final int[] types) {
		final StringBuilder s = new StringBuilder(30);
		s.append(types.length < 2 ? "" : "any type in [");
		for (int i = 0; i < types.length; i++) {
			s.append(getTypeString(types[i]));

			if (i != types.length - 1) {
				s.append(", ");
			}
		}
		if (types.length >= 2) {
			s.append("]");
		}
		return s.toString();
	}

}
