/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.icqaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>IcqAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the ICQ protocol. It should allow
 * the user to create and configure a new ICQ account.
 * 
 * @author Yana Stamcheva
 */
public class IcqAccountRegistrationWizard
    implements AccountRegistrationWizard
{
    /**
     * The logger.
     */
    private final Logger logger
        = Logger.getLogger(IcqAccountRegistrationWizard.class);

    private FirstWizardPage firstWizardPage;

    private IcqAccountRegistration registration = new IcqAccountRegistration();

    private WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private boolean isModification;

    /**
     * Creates an instance of <tt>IcqAccountRegistrationWizard</tt>.
     * 
     * @param wizardContainer the wizard container, where this wizard is added
     */
    public IcqAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;

        this.wizardContainer
            .setFinishButtonText(Resources.getString("service.gui.SIGN_IN"));
    }

    /**
     * Returns the protocol icon that will be shown on the left of the protocol
     * name in the list, where user will choose the protocol to register to.
     * 
     * @return a short description of the protocol.
     */
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.ICQ_LOGO);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getPageImage</code>
     * method. Returns the image used to decorate the wizard page
     * 
     * @return byte[] the image used to decorate the wizard page
     */
    public byte[] getPageImage()
    {
        return Resources.getImage(Resources.PAGE_IMAGE);
    }

    /**
     * Returns the protocol name that will be shown in the list, where user
     * will choose the protocol to register to.
     * 
     * @return the protocol name.
     */
    public String getProtocolName()
    {
        return Resources.getString("plugin.icqaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Returns a short description of the protocol that will be shown on the
     * right of the protocol name in the list, where user will choose the
     * protocol to register to.
     * 
     * @return a short description of the protocol.
     */
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.icqaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of <tt>WizardPage</tt>-s for this
     * wizard. 
     * 
     * @return the set of <tt>WizardPage</tt>-s for this
     * wizard. 
     */
    public Iterator<WizardPage> getPages()
    {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();
        firstWizardPage = new FirstWizardPage(this);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns a set of key-value pairs that will represent the summary for
     * this wizard.
     * 
     * @return a set of key-value pairs that will represent the summary for
     * this wizard. 
     */
    public Iterator<Map.Entry<String,String>> getSummary()
    {
        Map<String, String> summaryTable
            = new LinkedHashMap<String, String>();

        summaryTable.put(Resources.getString("service.gui.USER_IDENTIFIER"),
                        registration.getUin());
        summaryTable.put(Resources.getString("service.gui.REMEMBER_PASSWORD"),
                Boolean.toString(registration.isRememberPassword()));

        return summaryTable.entrySet().iterator();
    }

    /**
     * Installs the account created through this wizard.
     * 
     * @return the <tt>ProtocolProviderService</tt> for the newly created
     * account.
     * @throws OperationFailedException 
     */
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return this.signin(registration.getUin(), registration
            .getPassword());
    }

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     *
     * @param userName the user name to sign in with
     * @param password the password to sign in with
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public ProtocolProviderService signin(String userName, String password)
        throws OperationFailedException
    {
        ProtocolProviderFactory factory =
            IcqAccRegWizzActivator.getIcqProtocolProviderFactory();

        return this.installAccount(factory, userName, password);
    }

    /**
     * Creates an account for the given user and password.
     * 
     * @param providerFactory the ProtocolProviderFactory which will create the
     *            account
     * @param user the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @throws OperationFailedException 
     */
    public ProtocolProviderService installAccount(
        ProtocolProviderFactory providerFactory,
        String user,
        String passwd)
        throws OperationFailedException
    {
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        accountProperties.put(ProtocolProviderFactory.ACCOUNT_ICON_PATH,
            "resources/images/protocol/icq/icq32x32.png");

        if (registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        if (isModification)
        {
            providerFactory.modifyAccount(  protocolProvider,
                                            accountProperties);

            this.isModification  = false;

            return protocolProvider;
        }

        try
        {
            AccountID accountID =
                providerFactory.installAccount(user, accountProperties);

            ServiceReference serRef =
                providerFactory.getProviderForAccount(accountID);

            protocolProvider =
                (ProtocolProviderService) IcqAccRegWizzActivator.bundleContext
                    .getService(serRef);
        }
        catch (IllegalStateException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Account already exists.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (Exception exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Failed to add account",
                OperationFailedException.GENERAL_ERROR);
        }

        return protocolProvider;
    }

    /**
     * Fills the UIN and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * 
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load
     *            the data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        this.isModification = true;

        this.protocolProvider = protocolProvider;

        this.registration = new IcqAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Indicates if this wizard is opened for modification or for creating a
     * new account.
     * 
     * @return <code>true</code> if this wizard is opened for modification and
     * <code>false</code> otherwise.
     */
    public boolean isModification()
    {
        return isModification;
    }

    /**
     * Returns the wizard container, where all pages are added.
     * 
     * @return the wizard container, where all pages are added
     */
    public WizardContainer getWizardContainer()
    {
        return wizardContainer;
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     * 
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public IcqAccountRegistration getRegistration()
    {
        return registration;
    }

    /**
     * Returns the size of this wizard.
     * @return the size of this wizard
     */
    public Dimension getSize()
    {
        return new Dimension(600, 500);
    }
    
    /**
     * Returns the identifier of the page to show first in the wizard.
     * @return the identifier of the page to show first in the wizard.
     */
    public Object getFirstPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Returns the identifier of the page to show last in the wizard.
     * @return the identifier of the page to show last in the wizard.
     */
    public Object getLastPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Sets the modification property to indicate if this wizard is opened for
     * a modification.
     * 
     * @param isModification indicates if this wizard is opened for modification
     * or for creating a new account. 
     */
    public void setModification(boolean isModification)
    {
        this.isModification = isModification;
    }

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return FirstWizardPage.USER_NAME_EXAMPLE;
    }

    /**
     * Indicates whether this wizard enables the simple "sign in" form shown
     * when the user opens the application for the first time. The simple
     * "sign in" form allows user to configure her account in one click, just
     * specifying her username and password and leaving any other configuration
     * as by default.
     * @return <code>true</code> if the simple "Sign in" form is enabled or
     * <code>false</code> otherwise.
     */
    public boolean isSimpleFormEnabled()
    {
        return true;
    }

    /**
     * Opens the browser on the account registration page.
     */
    public void webSignup()
    {
        IcqAccRegWizzActivator.getBrowserLauncher().openURL(
            "https://www.icq.com/register/");
    }

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return true;
    }

    /**
     * Indicates if a sign up form is supported by this wizard.
     *
     * @return <tt>true</tt> if a sign up form is supported by this wizard,
     * <tt>false</tt> - otherwise
     */
    public boolean isSignupSupported()
    {
        return false;
    }

    /**
     * Returns a simple account registration form that would be the first form
     * shown to the user. Only if the user needs more settings she'll choose
     * to open the advanced wizard, consisted by all pages.
     * 
     * @return a simple account registration form
     */
    public Object getSimpleForm()
    {
        firstWizardPage = new FirstWizardPage(this);

        return firstWizardPage.getSimpleForm();
    }

    /**
     * Indicates that the account corresponding to the given
     * <tt>protocolProvider</tt> has been removed.
     * @param protocolProvider the protocol provider that has been removed
     */
    public void accountRemoved(ProtocolProviderService protocolProvider) {}
}
