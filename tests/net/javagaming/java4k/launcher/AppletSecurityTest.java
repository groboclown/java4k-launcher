package net.javagaming.java4k.launcher;

import net.javagaming.java4k.launcher.applet.AppletConfiguration;
import net.javagaming.java4k.launcher.applet.AppletLifeCycleRunner;
import net.javagaming.java4k.launcher.cache.Cache;
import net.javagaming.java4k.launcher.cache.Resource;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.either;

/**
 * From the Oracle documentation:
 * http://docs.oracle.com/javase/tutorial/deployment/applet/security.html
 * <p/>
 * Sandbox applets are restricted to the security sandbox and can perform the following operations:
 * <ul>
 * <li>They can make network connections to the host they came from.
 * </li><li><i>(not applicable)</i> They can easily display HTML documents using the showDocument method of the java.applet.AppletContext class.
 * </li><li><i>(not applicable)</i> They can invoke public methods of other applets on the same page.
 * </li><li><i>(not applicable)</i> Applets that are loaded from the local file system (from a directory in the user's CLASSPATH) have none of the restrictions that applets loaded over the network do.
 * </li><li>They can read secure system properties. See System Properties for a list of secure system properties.
 * </li><li><i>(not applicable)</i> When launched by using JNLP, sandbox applets can also perform the following operations:
 * <ul>
 * <li>They can open, read, and save files on the client.
 * </li><li>They can access the shared system-wide clipboard.
 * </li><li>They can access printing functions.
 * </li><li>They can store data on the client, decide how applets should be downloaded and cached, and much more. See JNLP API for more information about developing applets by using the JNLP API.
 * </li></ul></li>
 * </ul>
 * Sandbox applets cannot perform the following operations:
 * <ul>
 * <li>They cannot access client resources such as the local filesystem, executable files, system clipboard, and printers.
 * </li><li>They cannot connect to or retrieve resources from any third party server (any server other than the server it originated from).
 * </li><li>They cannot load native libraries.
 * </li><li>They cannot change the SecurityManager.
 * </li><li>They cannot create a ClassLoader.
 * </li><li>They cannot read certain system properties. See System Properties for a list of forbidden system properties.
 * </li>
 * </ul>
 * <p/>
 * There are some implicit restrictions - an applet is restricted to looking
 * at threads only associated with itself.
 */
public class AppletSecurityTest {
    @Test
    public void testReflection() throws InterruptedException, IOException {
        List<Throwable> ret = runApplet("ReflectionApplet");
        assertThat("must have 1 error",
                ret.size(), is(1));
        assertThat(ret.get(0), is(instanceOf(AccessControlException.class)));
    }

    @Test
    public void testSecurityManagerReplacement() throws InterruptedException, IOException {
        List<Throwable> ret = runApplet("SecurityManagerReplacementApplet");
        assertThat("must have 1 error",
                ret.size(), is(1));
        assertThat(ret.get(0), is(
                either(instanceOf(SecurityException.class)).or(
                        instanceOf((AccessControlException.class)))));
    }

    @Test
    public void testConstructorBad() throws InterruptedException, IOException {
        List<Throwable> ret = runApplet("ConstructorBadApplet");
        assertThat("must have 1 error",
                ret.size(), is(1));
        assertThat(ret.get(0), is(instanceOf(SecurityException.class)));
    }


    @Test
    public void testSwingUtilitiesInvoke()
            throws IOException, InterruptedException {
        List<Throwable> ret = runApplet("SwingUtilitiesInvokeApplet");
        assertThat("must have 1 error",
                ret.size(), is(1));
        assertThat(ret.get(0), is(instanceOf(SecurityException.class)));
    }


    @Test
    public void testAccessController()
            throws IOException, InterruptedException {
        List<Throwable> ret = runApplet("AccessControllerApplet");
        assertThat("must have 1 error",
                ret.size(), is(1));
        assertThat(ret.get(0), is(instanceOf(SecurityException.class)));
    }


    private List<Throwable> runApplet(String appletName)
            throws InterruptedException, IOException {
        String className = getClass().getPackage().getName() + ".applets." +
                appletName;

        ThreadGroup tg = new ThreadGroup("Applet group");
        DefaultGameDescription source = new DefaultGameDescription();
        source.setName("name");
        GameDetail gd = new MockGameDetail(source, null);
        LauncherManager launcherManager = new LauncherManager(
                new ProgressPanel(), null);
        AppletConfiguration config = new AppletConfiguration(
                className, getJarUrl(), getJarUrl(), 10, 10, tg, gd);
        Security.setupOptions(new String[0]);
        AppletLifeCycleRunner runner = new AppletLifeCycleRunner(gd, config,
                launcherManager);
        try {
            runner.start(null, true);
        } catch (Error t) {
            return singleThrowable(t);
        } catch (RuntimeException t) {
            return singleThrowable(t);
        } finally {
            runner.destroy(new ChildProgressController(null, 1));
        }
        return runner.getExceptions();
    }


    private Resource getJarUrl() {
        return Cache.getInstance().getTopResource(new File(
                new File(System.getProperty("launcher.dir")),
                "../tests.jar").toURI(), true);
    }


    private List<Throwable> singleThrowable(Throwable t) {
        while (t.getCause() != t && t.getCause() != null) {
            t = t.getCause();
        }
        //t.printStackTrace();
        return Collections.singletonList(t);
    }

    private class MockGameDetail extends AbstractGameDetail {
        public MockGameDetail(GameDescription source, Resource detailsSource) {
            super(source, detailsSource);
        }

        @Override
        public GameConfiguration createGameConfiguration(ThreadGroup parentAppletThreadGroup) throws IOException {
            return null;
        }
    }
}
