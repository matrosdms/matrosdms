package net.schwehla.matrosdms.rcp;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import net.schwehla.matrosdms.rcp.preferences.ScopedPreferenceStore;

public class MatrosActivator implements BundleActivator {
	
    public static final String PLUGIN_ID = "net.schwehla.matrosdms.rcp"; //$NON-NLS-1$

    private static MatrosActivator instance;

    private Bundle bundle;
    
    private IPreferenceStore preferenceStore;

    public MatrosActivator()
    {
        super();
        instance = this;
        
        
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        bundle = context.getBundle();

        setupProxyAuthenticator();

        preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, MyGlobalConstants.Preferences.NODE_COM_MATROSDMS);
        
        
        
        
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
    	
        if (preferenceStore != null && preferenceStore.needsSaving())
            ((ScopedPreferenceStore) preferenceStore).save();

         Job.getJobManager().cancel(null);
    }

    private void setupProxyAuthenticator()
    {
        // http://stackoverflow.com/questions/1626549/authenticated-http-proxy-with-java/16340273#16340273
        // Java ignores http.proxyUser. Here come's the workaround.
        Authenticator.setDefault(new Authenticator()
        {
            @SuppressWarnings("nls")
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                if (getRequestorType() == RequestorType.PROXY)
                {
                    String protocol = getRequestingProtocol().toLowerCase();
                    String host = System.getProperty(protocol + ".proxyHost", "");
                    String port = System.getProperty(protocol + ".proxyPort", "80");
                    String user = System.getProperty(protocol + ".proxyUser", "");
                    String password = System.getProperty(protocol + ".proxyPassword", "");

                    if (getRequestingHost().equalsIgnoreCase(host) && Integer.parseInt(port) == getRequestingPort())
                        return new PasswordAuthentication(user, password.toCharArray());
                }
                return null;
            }
        });
    }

    public Bundle getBundle()
    {
        return bundle;
    }

    public IPreferenceStore getPreferenceStore()
    {
        return preferenceStore;
    }

    public IPath getStateLocation()
    {
        return Platform.getStateLocation(bundle);
    }

    public static MatrosActivator getDefault()
    {
        return instance;
    }

    public static final void log(IStatus status)
    {
        Platform.getLog(FrameworkUtil.getBundle(MatrosActivator.class)).log(status);
    }

    public static void log(Throwable t)
    {
        log(new Status(Status.ERROR, PLUGIN_ID, t.getMessage(), t));
    }

    public static void log(String message)
    {
        log(new Status(Status.ERROR, PLUGIN_ID, message));
    }

    public static void log(List<Exception> errors)
    {
        for (Exception e : errors)
            log(e);
    }

    public static boolean isDevelopmentMode()
    {
        return System.getProperty("osgi.dev") != null; //$NON-NLS-1$
    }
}
