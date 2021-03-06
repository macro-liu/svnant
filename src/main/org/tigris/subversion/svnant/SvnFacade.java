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

import org.tigris.subversion.svnant.commands.SvnCommand;

import org.tigris.subversion.svnclientadapter.ISVNClientAdapter;
import org.tigris.subversion.svnclientadapter.ISVNConflictResolver;
import org.tigris.subversion.svnclientadapter.ISVNPromptUserPassword;
import org.tigris.subversion.svnclientadapter.SVNClientException;
import org.tigris.subversion.svnclientadapter.SVNConflictDescriptor;
import org.tigris.subversion.svnclientadapter.SVNConflictResult;

import org.tigris.subversion.svnant.types.SvnSetting;

import org.apache.tools.ant.types.Reference;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

import java.util.TimeZone;

import java.io.File;

/**
 * This facade provides a reusable way to configure and access subversion clients.
 * 
 * @author Daniel Kasmeroglu
 *         <a href="mailto:daniel.kasmeroglu@kasisoft.net">daniel.kasmeroglu@kasisoft.net</a>
 *
 */
public class SvnFacade {

    private static final String  DEFAULT_DATEFORMATTER = "MM/dd/yyyy hh:mm a";

    private static final Boolean DEFAULT_FAILONERROR   = Boolean.TRUE;

    private static final String  KEY_FACADE            = "org.tigris.subversion.svnant.SvnFacade";

    private SvnSetting           setting               = new SvnSetting( null );
    private SvnSetting           refidsetting          = null;
    private Reference            refid                 = null;

    /**
     * Returns a facade which is associated with the supplied ant project.
     *
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     *              
     * @return   A new facade. Not <code>null</code>.
     */
    private static final SvnFacade getFacade( ProjectComponent component ) {
        // in general I would prefer to use the key by it's own but the code might
        // become invalid if the ant svn tasks are used in parallel for the same
        // project (which is unlikely to happen), so here we're providing the necessary 
        // distinction.
        if( component instanceof SvnCommand ) {
            // if a command is passed we're using the task for reference
            component = ((SvnCommand) component).getTask();
        }
        String    key    = KEY_FACADE + component.hashCode();
        SvnFacade result = (SvnFacade) component.getProject().getReference( key );
        if( result == null ) {
            result = new SvnFacade();
            component.getProject().addReference( key, result );
        }
        return result;
    }

    /**
     * Returns the settings used by this facade.
     *
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     * 
     * @return   The settings used by this facade. Not <code>null</code>.
     */
    private static final SvnSetting getSetting( ProjectComponent component ) {
        return getFacade( component ).setting;
    }

    private static final SvnSetting getRefidSetting( ProjectComponent component ) {
        SvnFacade facade = getFacade( component );
        if( facade.refidsetting == null ) {
            if( facade.refid != null ) {
                Object obj = facade.refid.getReferencedObject( component.getProject() );
                if( obj == null ) {
                    throw new BuildException( "The refid attribute value '" + facade.refid + "' doesn't refer to any object." );
                }
                if( !(obj instanceof SvnSetting) ) {
                    throw new BuildException( "The refid attribute value '" + facade.refid + "' has an unknown type [" + obj.getClass().getName() + "]." );
                }
                facade.refidsetting = (SvnSetting) obj;
            } else {
                facade.refidsetting = facade.setting;
            }
        }
        return facade.refidsetting;
    }

    /**
     * Changes the refid used to access a svnsetting instance.
     *
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     * @param refid       The id of the configuration which has to be used.
     */
    public static final void setRefid( ProjectComponent component, Reference refid ) {
        getFacade( component ).refid = refid;
    }

    /**
     * Returns the <code>ConflictResolution</code> specification used to handle conflicts.
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     * 
     * @return   The <code>ConflictResolution</code> specification used to handle conflicts.
     */
    private static final ConflictResolution getConflictResolution( ProjectComponent component ) {
        ConflictResolution result = getSetting( component ).getConflictResolution();
        if( result == null ) {
            result = getRefidSetting( component ).getConflictResolution();
        }
        return result;
    }
    
    private static final SvnClientType getClientType( ProjectComponent component ) {
        return getRefidSetting( component ).getClient();
    }

    /**
     * @see SvnSetting#getUsername()
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    private static final String getUsername( ProjectComponent component ) {
        String result = getSetting( component ).getUsername();
        if( result == null ) {
            result = getRefidSetting( component ).getUsername();
        }
        return result;
    }

    /**
     * @see SvnSetting#getPassword()
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    private static final String getPassword( ProjectComponent component ) {
        String result = getSetting( component ).getPassword();
        if( result == null ) {
            result = getRefidSetting( component ).getPassword();
        }
        return result;
    }

    /**
     * @see SvnSetting#getSSLPassword()
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    private static final String getSSLPassword( ProjectComponent component ) {
        String result = getSetting( component ).getSSLPassword();
        if( result == null ) {
            result = getRefidSetting( component ).getSSLPassword();
        }
        return result;
    }
    
    /**
     * @see SvnSetting#getSSLClientCertPath()
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    private static final File getSSLClientCertPath( ProjectComponent component ) {
        File result = getSetting( component ).getSSLClientCertPath();
        if( result == null ) {
            result = getRefidSetting( component ).getSSLClientCertPath();
        }
        return result;
    }
    
    /**
     * @see SvnSetting#getSSHPort()
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    private static final Integer getSSHPort( ProjectComponent component ) {
        Integer result = getSetting( component ).getSSHPort();
        if( result == null ) {
            result = getRefidSetting( component ).getSSHPort();
        }
        return result;
    }
    
    /**
     * @see SvnSetting#getSSHPassphrase()
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    private static final String getSSHPassphrase( ProjectComponent component ) {
        String result = getSetting( component ).getSSHPassphrase();
        if( result == null ) {
            result = getRefidSetting( component ).getSSHPassphrase();
        }
        return result;
    }
    
    /**
     * @see SvnSetting#getSSHKeyPath()
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    private static final File getSSHKeyPath( ProjectComponent component ) {
        File result = getSetting( component ).getSSHKeyPath();
        if( result == null ) {
            result = getRefidSetting( component ).getSSHKeyPath();
        }
        return result;
    }
    
    /**
     * @see SvnSetting#getConfigDirectory()
     */
    private static final File getConfigDirectory( ProjectComponent component ) {
        File result = getSetting( component ).getConfigDirectory();
        if( result == null ) {
            result = getRefidSetting( component ).getConfigDirectory();
        }
        return result;
    }

    /**
     * @see SvnSetting#getCertReject()
     *  
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    private static final Boolean getCertReject( ProjectComponent component ) {
        Boolean result = getSetting( component ).getCertReject();
        if( result == null ) {
            result = getRefidSetting( component ).getCertReject();
        }
        return result;
    }

    /**
     * @see SvnSetting#getDateFormatter()
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    public static final String getDateFormatter( ProjectComponent component ) {
        String result = getSetting( component ).getDateFormatter();
        if( (result == null) || (result.length() == 0) ) {
            result = getRefidSetting( component ).getDateFormatter();
        }
        if( (result == null) || (result.length() == 0) ) {
            result = DEFAULT_DATEFORMATTER;
        }
        return result;
    }

    /**
     * @see SvnSetting#getDateTimezone()
     * 
     * @param component   The ant project component used to access the facade. Not <code>null</code>.
     */
    public static final TimeZone getDateTimezone( ProjectComponent component ) {
        String zone = getSetting( component ).getDateTimezone();
        if( (zone == null) || (zone.length() == 0) ) {
            zone = getRefidSetting( component ).getDateTimezone();
        }
        if( (zone == null) || (zone.length() == 0) ) {
            return null;
        } else {
            return TimeZone.getTimeZone( zone );
        }
    }

    /**
     * Returns <code>true</code> if a failure shall abort the build process.
     *
     * @param component  The ant project component used to access the facade. 
     *                          Not <code>null</code>.
     *                          
     * @return   <code>true</code> <=> A failure has to abort the build process.
     */
    public static final boolean getFailonerror( ProjectComponent component ) {
        Boolean result = getSetting( component ).getFailonerror();
        if( result == null ) {
            result = getRefidSetting( component ).getFailonerror();
        }
        if( result == null ) {
            result = DEFAULT_FAILONERROR;
        }
        return result.booleanValue();
    }

    /**
     * This method returns a SVN client adapter, based on the property set to the svn task. 
     * More specifically, the 'javahl' and 'svnkit' flags are verified, as well as the
     * availability of JAVAHL ad SVNKit adapters, to decide what flavour to use.
     * 
     * @param component  The ant project component used to access the facade. 
     *                   Not <code>null</code>.
     *                      
     * @return  An instance of SVN client adapter that meets the specified constraints, if any.
     *          Not <code>null</code>.
     *          
     * @throws BuildException   Thrown in a situation where no adapter can fit the constraints.
     */
    public static final synchronized ISVNClientAdapter getClientAdapter( ProjectComponent component ) throws BuildException {
        SvnClientType     clienttype = getClientType( component );
        ISVNClientAdapter result     = clienttype.createClient();
        File configdir = getConfigDirectory( component );
        if( configdir != null ) {
            try {
                result.setConfigDirectory( configdir );
            } catch( SVNClientException ex ) {
                throw new BuildException( "Failed to change the configuration directory to '" + configdir.getAbsolutePath() + "' !", ex );
            }
        }
        ConflictResolution conflictresolution = getConflictResolution( component );
        if( conflictresolution != null ) {
            result.addConflictResolutionCallback( new DefaultConflictResolver( conflictresolution ) );
        }
        if( getUsername( component ) != null ) {
            result.setUsername( getUsername( component ) );
        }
        if( clienttype == SvnClientType.cli ) {
            if( getPassword( component ) != null ) {
                result.setPassword( getPassword( component ) );
            }
        } else {
            result.addPasswordCallback( 
                new DefaultPasswordCallback( 
                    getUsername          ( component ),
                    getPassword          ( component ),
                    getSSHKeyPath        ( component ),
                    getSSHPassphrase     ( component ),
                    getSSHPort           ( component ),
                    getSSLClientCertPath ( component ),
                    getSSLPassword       ( component ),
                    getCertReject        ( component )
                ) 
            );
        }
        return result;
        
    }
    
    private static class DefaultConflictResolver implements ISVNConflictResolver {

        private ConflictResolution   choice;
        
        public DefaultConflictResolver( ConflictResolution resolve ) {
            choice = resolve;
        }
        
        /**
         * {@inheritDoc}
         */
        public SVNConflictResult resolve( SVNConflictDescriptor descriptor ) throws SVNClientException {
            return choice.resolve( descriptor );
        }
        
    } /* ENDCLASS */
    
    private static class DefaultPasswordCallback implements ISVNPromptUserPassword {

        private String    username;
        private String    password;
        private String    sshprivatekeypath;
        private String    sshpassphrase;
        private String    sslclientcertpath;
        private String    sslcertpassword;
        private Integer   sshport;
        private Boolean   reject;
        
        public DefaultPasswordCallback(
            String    newusername, 
            String    newpassword, 
            File      sshkeypath, 
            String    sshphrase, 
            Integer   sshp, 
            File      sslpath, 
            String    sslpassword,
            Boolean   certreject
        ) {
            username            = newusername;
            password            = newpassword;
            sshprivatekeypath   = sshkeypath != null ? sshkeypath.getAbsolutePath() : null;
            sshpassphrase       = sshphrase;
            sshport             = sshp;
            sslclientcertpath   = sslpath != null ? sslpath.getAbsolutePath() : null;
            sslcertpassword     = sslpassword;
            reject              = certreject;
        }
        
        
        /**
         * {@inheritDoc}
         */
        public boolean promptSSL( String realm, boolean maysave ) {
            return (sslclientcertpath != null) || (sslcertpassword != null);
        }

        /**
         * {@inheritDoc}
         */
        public String getSSLClientCertPassword() {
            return sslclientcertpath;
        }

        /**
         * {@inheritDoc}
         */
        public String getSSLClientCertPath() {
            return sslcertpassword;
        }
        
        /**
         * {@inheritDoc}
         */
        public int askTrustSSLServer( String info, boolean allowpermanently ) {
            if( Boolean.TRUE.equals( reject ) ) {
                return ISVNPromptUserPassword.Reject;
            } else {
                return ISVNPromptUserPassword.AcceptTemporary;
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public String getSSHPrivateKeyPath() {
            return sshprivatekeypath;
        }

        /**
         * {@inheritDoc}
         */
        public String getSSHPrivateKeyPassphrase() {
            return sshpassphrase;
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean promptSSH( String realm, String username, int sshport, boolean maysave ) {
            if( sshport > 0 ) {
                // the port is obviously given by the url
                this.sshport = Integer.valueOf( sshport );
            }
            return (sshpassphrase != null) || (sshprivatekeypath != null);
        }
        
        /**
         * {@inheritDoc}
         */
        public int getSSHPort() {
            if( sshport == null ) {
                return 22;
            } else {
                return sshport.intValue();
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean userAllowedSave() {
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        public String getUsername() {
            return username;
        }

        /**
         * {@inheritDoc}
         */
        public String getPassword() {
            return password;
        }

        /**
         * {@inheritDoc}
         */
        public String askQuestion( String realm, String question, boolean showanswer, boolean maysave ) {
            return "";
        }

        /**
         * {@inheritDoc}
         */
        public boolean prompt( String realm, String username, boolean maysave ) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public boolean promptUser( String realm, String username, boolean maysave ) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public boolean askYesNo( String realm, String question, boolean yesisdefault ) {
            return yesisdefault;
        }
        
    } /* ENDCLASS */

} /* ENDCLASS */
