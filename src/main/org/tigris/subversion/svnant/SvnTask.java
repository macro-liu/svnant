/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */ 
package org.tigris.subversion.svnant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNNotifyListener;
import org.tigris.subversion.svnclientadapter.SVNClientAdapterFactory;

/**
 * Svn Task
 * @author C�dric Chabanois 
 *         <a href="mailto:cchabanois@ifrance.com">cchabanois@ifrance.com</a>
 *
 */
public class SvnTask extends Task {
    private String username = null;
    private String password = null;
    private List commands = new ArrayList();
    private int logLevel = 0;
    private File logFile = new File("svn.log");
    private boolean javahl = true;
    private List notifyListeners = new ArrayList();
    private static boolean javahlAvailable = 
        SVNClientAdapterFactory.isSVNClientAvailable(SVNClientAdapterFactory.JAVAHL_CLIENT);
    private static boolean commandLineAvailable = 
        SVNClientAdapterFactory.isSVNClientAvailable(SVNClientAdapterFactory.COMMANDLINE_CLIENT);

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * set javahl to false to use command line interface
     * @param javahl
     */
    public void setJavahl(boolean javahl) {
        this.javahl = javahl;
    }

	public void setLogLevel(int logLevel) {
		if (logLevel > 3)
			logLevel = 3;
		else
		if (logLevel < 0)
			logLevel = 0;
		this.logLevel = logLevel;
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

    public void addCheckout(Checkout a) {
        commands.add(a);
    }

    public void addAdd(Add a) {
        commands.add(a);
    }

    public void addCommit(Commit a) {
        commands.add(a);
    }

    public void addCopy(Copy a) {
        commands.add(a);
    }

    public void addDelete(Delete a) {
        commands.add(a);
    }

    public void addExport(Export a) {
        commands.add(a);
    }

    public void addImport(Import a) {
        commands.add(a);
    }

    public void addMkdir(Mkdir a) {
        commands.add(a);
    }

    public void addMove(Move a) {
        commands.add(a);
    }

    public void addUpdate(Update a) {
        commands.add(a);
    }
    
    public void addPropset(Propset a) {
        commands.add(a);
    }
    
    public void addDiff(Diff a) {
        commands.add(a);
    }

    public void addKeywordsSet(Keywordsset a) {
        commands.add(a);
    }
    
    public void addKeywordsAdd(Keywordsadd a) {
        commands.add(a);
    }
    
    public void addKeywordsRemove(Keywordsremove a) {
        commands.add(a);
    }    

    public void addRevert(Revert a) {
        commands.add(a);
    }

    public void addCat(Cat a) {
        commands.add(a);
    }

    public void addPropdel(Propdel a) {
        commands.add(a);
    }
    
    public void addIgnore(Ignore a) {
        commands.add(a);
    }

    public void addNotifyListener(ISVNNotifyListener notifyListener) {
        notifyListeners.add(notifyListener);
    }

    public void execute() throws BuildException {

        ISVNClientAdapter svnClient;
        
        if ((javahl) && (javahlAvailable)) {
            svnClient = SVNClientAdapterFactory.createSVNClient(SVNClientAdapterFactory.JAVAHL_CLIENT);
            log("Using javahl");
        }
        else
        if (commandLineAvailable) {
            svnClient = SVNClientAdapterFactory.createSVNClient(SVNClientAdapterFactory.COMMANDLINE_CLIENT);
            log("Using command line interface (experimental)");
        } 
        else
            throw new BuildException("Cannot use javahl nor command line svn client");
        

        if (username != null)
            svnClient.setUsername(username);

        if (password != null)
            svnClient.setPassword(password);

        // TODO : delete these methods from JhlClientAdepter and from doc ?             
//        if (logLevel != 0)
//			JhlClientAdapter.enableLogging(logLevel,logFile);

        for (int i = 0; i < notifyListeners.size();i++) {
            svnClient.addNotifyListener((ISVNNotifyListener)notifyListeners.get(i));
        }

        for (int i = 0; i < commands.size(); i++) {
            SvnCommand command = (SvnCommand) commands.get(i);
            Feedback feedback = new Feedback(command);
			svnClient.addNotifyListener(feedback);
            command.execute(svnClient);
            svnClient.removeNotifyListener(feedback);
        }
        
        // disable logging
//		if (logLevel != 0)
//			JhlClientAdapter.enableLogging(0,logFile);        
    }

}
