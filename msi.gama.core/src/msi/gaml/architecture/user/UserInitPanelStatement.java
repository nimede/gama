package msi.gaml.architecture.user;

import msi.gama.common.interfaces.IKeyword;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.*;
import msi.gaml.descriptions.IDescription;

@symbol(name = IKeyword.USER_INIT, kind = ISymbolKind.BEHAVIOR, with_sequence = true)
@inside(kinds = { ISymbolKind.SPECIES, ISymbolKind.EXPERIMENT, ISymbolKind.MODEL })
public class UserInitPanelStatement extends UserPanelStatement {

	public UserInitPanelStatement(final IDescription desc) {
		super(desc);
	}

}
