/*********************************************************************************************
 * 
 *
 * 'GamaTextFile.java', in plugin 'msi.gama.core', is part of the source code of the 
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 * 
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 * 
 * 
 **********************************************************************************************/
package msi.gama.util.file;

import java.io.*;

import rcaller.RCaller;
import rcaller.RCode;
import msi.gama.common.GamaPreferences;
import msi.gama.common.util.FileUtils;
import msi.gama.common.util.GuiUtils;
import msi.gama.precompiler.GamlAnnotations.file;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.*;
import msi.gama.util.matrix.IMatrix;
import msi.gaml.types.IType;
import com.vividsolutions.jts.geom.Envelope;

@file(name = "R", extensions = { "r" }, buffer_type = IType.MAP, buffer_content = IType.MAP)
public class RFile extends GamaFile< GamaMap<String, IList>, GamaPair<String, IList>, String, IList> {
	private final boolean DEBUG = false; // Change DEBUG = false for release version
	private final IContainer parameters;
	public RFile(final IScope scope, final String pathName) throws GamaRuntimeException {
		super(scope, pathName);
		parameters = null;
	}

	public RFile(final IScope scope, final String pathName, final IContainer p) {
		super(scope, pathName);
		parameters = p;
	}

	@Override
	public String _stringValue(final IScope scope) throws GamaRuntimeException {
		getContents(scope);
		StringBuilder sb = new StringBuilder(getBuffer().length(scope) * 200);
		for ( IList s : getBuffer().iterable(scope) ) {
			sb.append(s).append("\n"); // TODO Factorize the different calls to "new line" ...
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see msi.gama.util.GamaFile#fillBuffer()
	 */
	@Override
	protected void fillBuffer(final IScope scope) throws GamaRuntimeException {
		if ( getBuffer() != null ) { return; }
//		try {
//			final BufferedReader in = new BufferedReader(new FileReader(getFile()));
//			final GamaList<String> allLines = new GamaList();
//			String str;
//			str = in.readLine();
//			while (str != null) {
//				allLines.add(str);
//				str = in.readLine();
//			}
//			in.close();
//			setBuffer(allLines);
//		} catch (final IOException e) {
//			throw GamaRuntimeException.create(e);
//		}
		if( parameters == null ){
			doRFileEvaluate(scope);
		}else{
			doRFileEvaluate(scope, this.parameters);
		}
		
	}
	
	public void doRFileEvaluate(IScope scope, final IContainer param ){
		if ( param.length(scope) == 0 ) { throw GamaRuntimeException.error("Missing Parameter Exception"); }

		final String RFile = getPath();
		try {
			// Call R
			RCaller caller = new RCaller();

			String RPath = ((IGamaFile) GamaPreferences.LIB_R.value(scope)).getPath();
			caller.setRscriptExecutable(RPath);
			// caller.setRscriptExecutable("\"" + RPath + "\"");
			// if(java.lang.System.getProperty("os.name").startsWith("Mac"))
			// {
			// caller.setRscriptExecutable(RPath);
			// }

			double[] vectorParam = new double[param.length(scope)];

			int k = 0;
			for ( Object o : param.iterable(scope) ) {
				vectorParam[k++] = Double.parseDouble(o.toString());
			}

			RCode c = new RCode();
			// Adding the parameters
			c.addDoubleArray("vectorParam", vectorParam);

			// Adding the codes in file
			GamaList R_statements = new GamaList<String>();

			// tmthai.begin----------------------------------------------------------------------------
			String fullPath = FileUtils.constructAbsoluteFilePath(scope, RFile, true);
			if ( DEBUG ) {
				GuiUtils.debug("Stats.R_compute_param.RScript:" + RPath);
				GuiUtils.debug("Stats.R_compute_param.Param:" + vectorParam.toString());
				GuiUtils.debug("Stats.R_compute_param.RFile:" + RFile);
				GuiUtils.debug("Stats.R_compute_param.fullPath:" + fullPath);
			}

			// FileReader fr = new FileReader(RFile);
			FileReader fr = new FileReader(fullPath);
			// tmthai.end----------------------------------------------------------------------------

			BufferedReader br = new BufferedReader(fr);
			String statement;

			while ((statement = br.readLine()) != null) {
				c.addRCode(statement);
				R_statements.add(statement);
				// java.lang.System.out.println(statement);
			}
			br.close();
			fr.close();
			caller.setRCode(c);

			GamaMap<String, IList> result = new GamaMap();

			String var = computeVariable(R_statements.get(R_statements.length(scope) - 1).toString());
			caller.runAndReturnResult(var);

			// DEBUG:
			// java.lang.System.out.println("Name: '" + R_statements.length(scope) + "'");
			if ( DEBUG ) {
				GuiUtils.debug("Stats.R_compute_param.R_statements.length: '" + R_statements.length(scope) + "'");
			}

			for ( String name : caller.getParser().getNames() ) {
				Object[] results = null;
				results = caller.getParser().getAsStringArray(name);
				// java.lang.System.out.println("Name: '" + name + "'");
				if ( DEBUG ) {
					GuiUtils.debug("Stats.R_compute_param.caller.Name: '" + name + "' length: " + results.length +
						" - Value: " + results.toString());
				}

				// for (int i = 0; i < results.length; i++) {
				// //java.lang.System.out.println(results[i]);
				// if (DEBUG) GuiUtils.debug(results[i].toString());
				// //java.lang.System.out.println("Name: '" + name + "'");
				// }
				result.put(name, new GamaList(results));
			}

			if ( DEBUG ) {
				GuiUtils.debug("Stats.R_compute_param.return:" + result.toGaml());
			}
			
			setBuffer(result);

		} catch (Exception ex) {

			throw GamaRuntimeException.error("RCallerExecutionException " + ex.getMessage());
		}
	}
	
	public void doRFileEvaluate(IScope scope){		
		final String RFile = getPath();
		try {
			// Call R
			RCaller caller = new RCaller();

			String RPath = ((IGamaFile) GamaPreferences.LIB_R.value(scope)).getPath();
			caller.setRscriptExecutable(RPath);
			// caller.setRscriptExecutable("\"" + RPath + "\"");
			// if(java.lang.System.getProperty("os.name").startsWith("Mac"))
			// {
			// caller.setRscriptExecutable(RPath);
			// }

			RCode c = new RCode();
			GamaList R_statements = new GamaList<String>();

			// tmthai.begin----------------------------------------------------------------------------
			String fullPath = FileUtils.constructAbsoluteFilePath(scope, RFile, true);
			if ( DEBUG ) {
				GuiUtils.debug("Stats.R_compute.RScript:" + RPath);
				GuiUtils.debug("Stats.R_compute.RFile:" + RFile);
				GuiUtils.debug("Stats.R_compute.fullPath:" + fullPath);
			}

			// FileReader fr = new FileReader(RFile);
			FileReader fr = new FileReader(fullPath);
			// tmthai.end----------------------------------------------------------------------------

			BufferedReader br = new BufferedReader(fr);
			String statement;
			while ((statement = br.readLine()) != null) {
				c.addRCode(statement);
				R_statements.add(statement);
				// java.lang.System.out.println(statement);
				if ( DEBUG ) {
					GuiUtils.debug("Stats.R_compute.statement:" + statement);
				}

			}

			fr.close();
			br.close();
			caller.setRCode(c);

			GamaMap<String, IList> result = new GamaMap();

			String var = computeVariable(R_statements.get(R_statements.length(scope) - 1).toString());
			caller.runAndReturnResult(var);
			for ( String name : caller.getParser().getNames() ) {
				Object[] results = null;
				results = caller.getParser().getAsStringArray(name);
				// for (int i = 0; i < results.length; i++) {
				// java.lang.System.out.println(results[i]);
				// }
				if ( DEBUG ) {
					GuiUtils.debug("Stats.R_compute_param.caller.Name: '" + name + "' length: " + results.length +
						" - Value: " + results.toString());
				}
				result.put(name, new GamaList(results));
			}
			if ( DEBUG ) {
				GuiUtils.debug("Stats.R_compute.return:" + result.toGaml());
			}
//			return result;
			setBuffer(result);


		} catch (Exception ex) {

			throw GamaRuntimeException.error("RCallerExecutionException " + ex.getMessage());
		}
	}


	private static String computeVariable(final String string) {
		String[] tokens = string.split("<-");
		return tokens[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see msi.gama.util.GamaFile#flushBuffer()
	 */
	@Override
	protected void flushBuffer() throws GamaRuntimeException {
		// TODO A faire.

	}

	@Override
	public Envelope computeEnvelope(final IScope scope) {
		return null;
	}

}
