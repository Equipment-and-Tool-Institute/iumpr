/**
 * Copyright 2017 Equipment & Tool Institute
 */
package net.soliddesign.iumpr.ui;

import static net.soliddesign.iumpr.IUMPR.NL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.etools.j1939tools.bus.Adapter;
import org.etools.j1939tools.bus.BusException;
import org.etools.j1939tools.bus.RP1210;
import org.etools.j1939tools.bus.RP1210Bus;
import org.etools.j1939tools.j1939.J1939;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.soliddesign.iumpr.TestExecutor;
import net.soliddesign.iumpr.controllers.CollectResultsController;
import net.soliddesign.iumpr.controllers.Controller;
import net.soliddesign.iumpr.controllers.DataPlateController;
import net.soliddesign.iumpr.controllers.MonitorCompletionController;
import net.soliddesign.iumpr.controllers.ResultsListener;
import net.soliddesign.iumpr.modules.ComparisonModule;
import net.soliddesign.iumpr.modules.ReportFileModule;
import net.soliddesign.iumpr.ui.help.HelpView;

/**
 * Unit testing the {@link UserInterfaceController}
 *
 * @author Matt Gumbel (matt@soliddesign.net)
 *
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", justification = "The values returned are properly ignored on verify statements.")
@RunWith(MockitoJUnitRunner.class)
public class UserInterfaceControllerTest {

    private Adapter adapter1 = new Adapter("Adapter1", "SD", (short) 1);

    private Adapter adapter2 = new Adapter("Adapter2", "SD", (short) 2);

    @Mock
    private CollectResultsController collectResultsController;

    @Mock
    private ComparisonModule comparisonModule;

    @Mock
    private DataPlateController dataPlateController;

    private TestExecutor executor;

    @Mock
    private HelpView helpView;

    private UserInterfaceController instance;

    @Mock
    private MonitorCompletionController monitorCompletionController;

    @Mock
    private ReportFileModule reportFileModule;

    @Mock
    private RP1210 rp1210;

    @Mock
    private RP1210Bus rp1210Bus;

    @Mock
    private Runtime runtime;

    private Thread shutdownHook;

    @Mock
    private UserInterfaceView view;

    private File getAsciiFile() throws IOException {
        File file = File.createTempFile("test", ".iumpr");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                Charset.forName("UTF-8").newEncoder());) {
            writer.write("this is" + NL + " a bunch of" + NL + " ascii");
            return file;
        }
    }

    @Before
    public void setUp() throws Exception {
        executor = new TestExecutor();
        List<Adapter> adapters = new ArrayList<>();
        adapters.add(adapter1);
        adapters.add(adapter2);
        when(rp1210.getAdapters()).thenReturn(adapters);
        // when(rp1210.setAdapter(any(), eq(0xF9))).thenReturn(rp1210Bus);

        instance = new UserInterfaceController(view, dataPlateController, collectResultsController,
                monitorCompletionController, comparisonModule, rp1210, reportFileModule, runtime, executor, helpView);
        ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
        verify(runtime).addShutdownHook(captor.capture());
        shutdownHook = captor.getValue();
    }

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(collectResultsController, dataPlateController, monitorCompletionController,
                reportFileModule, rp1210, rp1210Bus, runtime, comparisonModule, view, helpView);
    }

    @Test
    public void testDisconnect() throws Exception {
        instance.onAdapterComboBoxItemSelected(adapter1);
        executor.run();

        instance.disconnect();

        verify(comparisonModule).reset();
        verify(comparisonModule).setJ1939(any(J1939.class));

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Connecting to Adapter");
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Report File");

        verify(rp1210Bus).stop();
        verify(rp1210).getAdapters();
        // verify(rp1210).setAdapter(adapter1, 0xF9);
    }

    @Test
    public void testDisconnectHandlesException() throws Exception {
        instance.onAdapterComboBoxItemSelected(adapter1);
        executor.run();

        Mockito.doThrow(new BusException("Testing")).when(rp1210Bus).stop();

        instance.disconnect();

        verify(comparisonModule).reset();
        verify(comparisonModule).setJ1939(any(J1939.class));

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Connecting to Adapter");
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Report File");

        verify(rp1210Bus).stop();
        verify(rp1210).getAdapters();
        // verify(rp1210).setAdapter(adapter1, 0xF9);
    }

    @Test
    public void testDisconnectWithNull() {
        instance.disconnect();
        // Nothing bad happens
    }

    @Test
    public void testGetAdapters() throws Exception {
        List<Adapter> adapters = instance.getAdapters();
        assertEquals(2, adapters.size());
        assertEquals("Adapter1", adapters.get(0).getName());
        assertEquals("Adapter2", adapters.get(1).getName());
        verify(rp1210).getAdapters();
    }

    @Test
    public void testGetAdaptersHandlesException() throws Exception {
        when(rp1210.getAdapters()).thenThrow(new BusException("Surprise", new Exception()));
        assertEquals(0, instance.getAdapters().size());

        // Doesn't happen again
        assertEquals(0, instance.getAdapters().size());

        verify(rp1210).getAdapters();

        verify(view).displayDialog("The List of Communication Adapters could not be loaded.", "Failure",
                JOptionPane.ERROR_MESSAGE, false);
    }

    @Test
    public void testGetReportFileModule() {
        assertSame(reportFileModule, instance.getReportFileModule());
    }

    @Test
    public void testOnAdapterComboBoxItemSelectedWithFile() throws Exception {
        File file = File.createTempFile("test", ".iumpr");
        instance.setReportFile(file);
        when(rp1210Bus.getAddress()).thenReturn(0xF9);

        instance.onAdapterComboBoxItemSelected(adapter1);
        executor.run();

        J1939 actual = instance.getNewJ1939();
        assertEquals(0xF9, actual.getBusAddress());

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Connecting to Adapter");
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Push Read Vehicle Info Button");
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(true);

        verify(comparisonModule).reset();
        verify(comparisonModule).setJ1939(any(J1939.class));

        verify(reportFileModule).setReportFile(any(ResultsListener.class), eq(file), eq(false));

        verify(rp1210).getAdapters();
        // verify(rp1210).setAdapter(adapter1, 0xF9);
        verify(rp1210Bus).getAddress();
    }

    @Test
    public void testOnAdapterComboBoxItemSelectedWithNoFile() throws Exception {
        instance.setReportFile(null);

        instance.onAdapterComboBoxItemSelected(adapter1);
        executor.run();

        assertEquals("Adapter1", instance.getSelectedAdapter().getName());

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Connecting to Adapter");
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Report File");

        verify(comparisonModule).reset();
        verify(comparisonModule).setJ1939(any(J1939.class));
        verify(reportFileModule).setReportFile(any(ResultsListener.class), eq(null), eq(false));

        verify(rp1210).getAdapters();
        // verify(rp1210).setAdapter(adapter1, 0xF9);
    }

    @Test
    public void testOnCollectTestResultsButtonClickedAndListenerResponse() throws Exception {
        when(collectResultsController.isActive()).thenReturn(true);

        instance.onCollectTestResultsButtonClicked();
        ArgumentCaptor<ResultsListener> argumentCaptor = ArgumentCaptor.forClass(ResultsListener.class);
        verify(collectResultsController).execute(argumentCaptor.capture(), any(J1939.class), eq(reportFileModule));

        verify(view).setCollectTestResultsButtonEnabled(false);
        verify(view).setMonitorCompletionButtonEnabled(false);
        verify(view, never()).setGenerateDataPlateButtonEnabled(true);

        ResultsListener listener = argumentCaptor.getValue();
        listener.onProgress(50, 100, "Half");
        verify(view).setProgressBarValue(0, 100, 50);
        verify(view).setProgressBarText("Half");

        listener.onResult(Collections.singletonList("Result"));
        verify(view).appendResults("Result" + NL);

        listener.onComplete(true);
        verify(view).setCollectTestResultsButtonEnabled(true);
        verify(view).setMonitorCompletionButtonEnabled(true);

        verify(collectResultsController).isActive();
        verify(collectResultsController).stop();
    }

    @Test
    public void testOnFileChosenExistingFileIsReadOnly() throws Exception {
        File file = getAsciiFile();
        file.setWritable(false);

        instance.onFileChosen(file);
        executor.run();

        assertNull(instance.getReportFile());
        verify(view).displayDialog(
                "File cannot be used." + NL + "File cannot be written" + NL + "Please select a different file.",
                "File Error",
                JOptionPane.ERROR_MESSAGE, false);

        verify(comparisonModule).reset();

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Scanning Report File");
        inOrder.verify(view).setSelectFileButtonText(null);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Vehicle Adapter");
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnFileChosenExistingFileWithProblem() throws Exception {
        File file = getAsciiFile();

        Mockito.doThrow(new IOException("There was a failure")).when(reportFileModule)
                .setReportFile(any(ResultsListener.class), eq(file), eq(false));

        instance.onFileChosen(file);
        executor.run();

        assertNull(instance.getReportFile());
        verify(view).displayDialog(
                "File cannot be used." + NL + "There was a failure" + NL + "Please select a different file.",
                "File Error",
                JOptionPane.ERROR_MESSAGE, false);
        verify(reportFileModule).setReportFile(any(ResultsListener.class), eq(file), eq(false));

        verify(comparisonModule).reset();

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Scanning Report File");
        inOrder.verify(view).setSelectFileButtonText(null);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Vehicle Adapter");
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnFileChosenNewFileButSameNameAsExistingFileIsReadOnly() throws Exception {
        File file = getAsciiFile();
        file.setWritable(false);

        File newFile = new File(file.getAbsolutePath().replace(".iumpr", ""));
        instance.onFileChosen(newFile);
        executor.run();

        assertNull(instance.getReportFile());
        verify(view).displayDialog(
                "File cannot be used." + NL + "File cannot be written" + NL + "Please select a different file.",
                "File Error",
                JOptionPane.ERROR_MESSAGE, false);

        verify(comparisonModule).reset();

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Scanning Report File");
        inOrder.verify(view).setSelectFileButtonText(null);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Vehicle Adapter");
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnFileChosenNewFileWithExtensionCreated() throws Exception {
        File file = mock(File.class);
        String path = "file\\location\\name.iumpr";
        when(file.exists()).thenReturn(false);
        when(file.getName()).thenReturn("name.iumpr");
        when(file.getAbsolutePath()).thenReturn(path);
        when(file.createNewFile()).thenReturn(true);

        instance.onFileChosen(file);
        executor.run();

        File reportFile = instance.getReportFile();
        assertNotNull(reportFile);
        assertEquals(path, reportFile.getAbsolutePath());
        verify(view).setSelectFileButtonText(path);

        verify(reportFileModule).setReportFile(any(ResultsListener.class), eq(file), eq(true));

        verify(comparisonModule).reset();

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Scanning Report File");
        inOrder.verify(view).setSelectFileButtonText(file.getAbsolutePath());
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Vehicle Adapter");
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnFileChosenNewFileWithExtensionNotCreated() throws Exception {
        File file = mock(File.class);
        when(file.exists()).thenReturn(false);
        when(file.getName()).thenReturn("name.iumpr");
        when(file.createNewFile()).thenReturn(false);

        instance.onFileChosen(file);
        executor.run();

        assertNull(instance.getReportFile());
        verify(view).displayDialog(
                "File cannot be used." + NL + "File cannot be created" + NL + "Please select a different file.",
                "File Error",
                JOptionPane.ERROR_MESSAGE, false);

        verify(comparisonModule).reset();

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Scanning Report File");
        inOrder.verify(view).setSelectFileButtonText(file.getAbsolutePath());
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Vehicle Adapter");
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);

    }

    @Test
    public void testOnFileChosenNewFileWithoutExtensionCreated() throws Exception {
        File tempFile = File.createTempFile("testing", ".txt");
        assertTrue(tempFile.delete());
        File file = new File(tempFile.getAbsolutePath());

        instance.onFileChosen(file);
        executor.run();

        File reportFile = instance.getReportFile();
        assertNotNull(reportFile);
        assertTrue(reportFile.getAbsolutePath().endsWith(tempFile.getName() + ".iumpr"));

        verify(reportFileModule).setReportFile(any(ResultsListener.class), eq(reportFile), eq(true));

        verify(comparisonModule).reset();

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Scanning Report File");
        inOrder.verify(view).setSelectFileButtonText(reportFile.getAbsolutePath());
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Vehicle Adapter");
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnFileChosenWithAdapter() throws Exception {
        instance.onAdapterComboBoxItemSelected(adapter1);
        executor.run();

        verify(rp1210).getAdapters();
        // verify(rp1210).setAdapter(adapter1, 0xF9);
        verify(comparisonModule).setJ1939(any(J1939.class));
        verify(comparisonModule).reset();

        InOrder inOrder1 = inOrder(view);
        inOrder1.verify(view).setVin("");
        inOrder1.verify(view).setEngineCals("");
        inOrder1.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder1.verify(view).setStopButtonEnabled(false);
        inOrder1.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder1.verify(view).setAdapterComboBoxEnabled(false);
        inOrder1.verify(view).setSelectFileButtonEnabled(false);
        inOrder1.verify(view).setProgressBarText("Connecting to Adapter");
        inOrder1.verify(view).setAdapterComboBoxEnabled(true);
        inOrder1.verify(view).setSelectFileButtonEnabled(true);
        inOrder1.verify(view).setProgressBarText("Select Report File");

        File file = File.createTempFile("test", ".iumpr");
        instance.onFileChosen(file);
        executor.run();

        verify(reportFileModule).setReportFile(any(ResultsListener.class), eq(file), eq(false));

        assertSame(file, instance.getReportFile());

        verify(comparisonModule, times(2)).reset();

        InOrder inOrder2 = inOrder(view);
        inOrder2.verify(view, times(2)).setVin("");
        inOrder2.verify(view).setEngineCals("");
        inOrder2.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder2.verify(view).setStopButtonEnabled(false);
        inOrder2.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder2.verify(view).setAdapterComboBoxEnabled(false);
        inOrder2.verify(view).setSelectFileButtonEnabled(false);
        inOrder2.verify(view).setProgressBarText("Scanning Report File");
        inOrder2.verify(view).setSelectFileButtonText(file.getAbsolutePath());
        inOrder2.verify(view).setAdapterComboBoxEnabled(true);
        inOrder2.verify(view).setSelectFileButtonEnabled(true);
        inOrder2.verify(view).setProgressBarText("Push Read Vehicle Info Button");
        inOrder2.verify(view).setReadVehicleInfoButtonEnabled(true);
        inOrder2.verify(view).setAdapterComboBoxEnabled(true);
        inOrder2.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnFileChosenWithNoAdapter() throws Exception {
        File file = File.createTempFile("test", ".iumpr");
        instance.onFileChosen(file);
        executor.run();

        verify(reportFileModule).setReportFile(any(ResultsListener.class), eq(file), eq(false));

        assertSame(file, instance.getReportFile());

        verify(comparisonModule).reset();

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarText("Scanning Report File");
        inOrder.verify(view).setSelectFileButtonText(file.getAbsolutePath());
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
        inOrder.verify(view).setProgressBarText("Select Vehicle Adapter");
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnGenerateDataPlateButtonClickedAndListenerResponseFailed() throws Exception {
        when(dataPlateController.isActive()).thenReturn(true);

        instance.onGenerateDataPlateButtonClicked();
        ArgumentCaptor<ResultsListener> argumentCaptor = ArgumentCaptor.forClass(ResultsListener.class);
        verify(dataPlateController).execute(argumentCaptor.capture(), any(J1939.class), eq(reportFileModule));

        verify(view).setReadVehicleInfoButtonEnabled(false);
        verify(view).setGenerateDataPlateButtonEnabled(false);

        ResultsListener listener = argumentCaptor.getValue();
        listener.onProgress(50, 100, "Half");
        verify(view).setProgressBarValue(0, 100, 50);
        verify(view).setProgressBarText("Half");

        // Not really necessary here, but it tests this method on the listener
        listener.onProgress("New Message");
        verify(view).setProgressBarText("New Message");

        listener.onResult(Collections.singletonList("Result"));
        verify(view).appendResults("Result" + NL);

        listener.onComplete(false);
        verify(view).setReadVehicleInfoButtonEnabled(true);
        verify(view).setGenerateDataPlateButtonEnabled(true);
        verify(view).setAdapterComboBoxEnabled(false);
        verify(view).setSelectFileButtonEnabled(false);

        verify(dataPlateController).isActive();
        verify(dataPlateController).stop();
    }

    @Test
    public void testOnGenerateDataPlateButtonClickedAndListenerResponseSuccess() throws Exception {
        when(dataPlateController.isActive()).thenReturn(true);

        instance.onGenerateDataPlateButtonClicked();
        ArgumentCaptor<ResultsListener> argumentCaptor = ArgumentCaptor.forClass(ResultsListener.class);
        verify(dataPlateController).execute(argumentCaptor.capture(), any(J1939.class), eq(reportFileModule));

        verify(view).setReadVehicleInfoButtonEnabled(false);
        verify(view).setGenerateDataPlateButtonEnabled(false);

        ResultsListener listener = argumentCaptor.getValue();
        listener.onProgress(50, 100, "Half");
        verify(view).setProgressBarValue(0, 100, 50);
        verify(view).setProgressBarText("Half");

        listener.onResult(Collections.singletonList("Result"));
        verify(view).appendResults("Result" + NL);

        listener.onComplete(true);
        verify(view).setCollectTestResultsButtonEnabled(true);
        verify(view).setMonitorCompletionButtonEnabled(true);
        verify(view).setAdapterComboBoxEnabled(false);
        verify(view).setSelectFileButtonEnabled(false);

        verify(dataPlateController).isActive();
        verify(dataPlateController).stop();
    }

    @Test
    public void testOnHelpButtonClicked() throws Exception {
        instance.onHelpButtonClicked();
        verify(helpView).setVisible(true);
    }

    @Test
    public void testOnMonitorCompletionButtonClickedAndListenerResponse() throws Exception {
        when(monitorCompletionController.isActive()).thenReturn(true);

        instance.onMonitorCompletionButtonClicked();
        ArgumentCaptor<ResultsListener> argumentCaptor = ArgumentCaptor.forClass(ResultsListener.class);
        verify(monitorCompletionController).execute(argumentCaptor.capture(), any(J1939.class), eq(reportFileModule));

        verify(view).setCollectTestResultsButtonEnabled(false);
        verify(view).setMonitorCompletionButtonEnabled(false);
        verify(view).setStatusViewVisible(true);

        verify(view, never()).setGenerateDataPlateButtonEnabled(true);

        ResultsListener listener = argumentCaptor.getValue();
        listener.onProgress(50, 100, "Half");
        verify(view).setProgressBarValue(0, 100, 50);
        verify(view).setProgressBarText("Half");

        listener.onResult(Collections.singletonList("Result"));
        verify(view).appendResults("Result" + NL);

        listener.onComplete(true);
        verify(view).setCollectTestResultsButtonEnabled(true);
        verify(view).setMonitorCompletionButtonEnabled(true);

        verify(monitorCompletionController).isActive();
        verify(monitorCompletionController).stop();
        verify(view).setStatusViewVisible(false);
    }

    @Test
    public void testOnReadVehicleInfoButtonClickedWithNullCals() throws Exception {
        when(comparisonModule.getVin()).thenReturn("12345678901234567890");
        when(comparisonModule.getCalibrationsAsString()).thenThrow(new IOException("Cals not read"));

        instance.onReadVehicleInfoButtonClicked();
        executor.run();

        verify(comparisonModule).getVin();
        verify(comparisonModule).getCalibrationsAsString();
        verify(comparisonModule).reset();

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarValue(0, 6, 1);
        inOrder.verify(view).setProgressBarText("Reading Vehicle Identification Number");
        inOrder.verify(view).setVin("12345678901234567890");
        inOrder.verify(view).setProgressBarValue(0, 6, 2);
        inOrder.verify(view).setProgressBarText("Reading Vehicle Calibrations");
        inOrder.verify(view).setProgressBarText("Cals not read");
        inOrder.verify(view).displayDialog("Cals not read", "Communications Error", JOptionPane.ERROR_MESSAGE, false);
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(true);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnReadVehicleInfoButtonClickedWithNullVin() throws Exception {
        when(comparisonModule.getVin()).thenThrow(new IOException("VIN not read"));

        instance.onReadVehicleInfoButtonClicked();
        executor.run();

        verify(comparisonModule).getVin();
        verify(comparisonModule).reset();
        verify(view).setVin("");
        verify(view).setEngineCals("");
        verify(view).setReadVehicleInfoButtonEnabled(false);
        verify(view, times(2)).setGenerateDataPlateButtonEnabled(false);
        verify(view, times(2)).setStopButtonEnabled(false);
        verify(view).setAdapterComboBoxEnabled(false);
        verify(view).setSelectFileButtonEnabled(false);

        verify(view).setProgressBarValue(0, 6, 1);
        verify(view).setProgressBarText("Reading Vehicle Identification Number");
        verify(view).setProgressBarText("VIN not read");
        verify(view).displayDialog("VIN not read", "Communications Error", JOptionPane.ERROR_MESSAGE, false);

        verify(view).setReadVehicleInfoButtonEnabled(true);
        verify(view).setAdapterComboBoxEnabled(true);
        verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnReadVehicleInfoButtonClickedWithReportFileDifferent() throws Exception {
        when(comparisonModule.getVin()).thenReturn("12345678901234567890");
        when(comparisonModule.getCalibrationsAsString()).thenReturn("Engine Cals");
        ArgumentCaptor<ResultsListener> listenerCaptor = ArgumentCaptor.forClass(ResultsListener.class);
        when(comparisonModule.compareFileToVehicle(listenerCaptor.capture(), eq(reportFileModule), eq(2), eq(6)))
                .thenReturn(false);

        instance.onReadVehicleInfoButtonClicked();
        executor.run();

        verify(comparisonModule).reset();
        verify(comparisonModule).getVin();
        verify(comparisonModule).getCalibrationsAsString();
        verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(6));

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarValue(0, 6, 1);
        inOrder.verify(view).setProgressBarText("Reading Vehicle Identification Number");
        inOrder.verify(view).setVin("12345678901234567890");
        inOrder.verify(view).setProgressBarValue(0, 6, 2);
        inOrder.verify(view).setProgressBarText("Reading Vehicle Calibrations");
        inOrder.verify(view).setEngineCals("Engine Cals");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(true);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnReadVehicleInfoButtonClickedWithReportFileMatched() throws Exception {
        when(comparisonModule.getVin()).thenReturn("12345678901234567890");
        when(comparisonModule.getCalibrationsAsString()).thenReturn("Engine Cals");
        when(comparisonModule.compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(6)))
                .thenReturn(true);

        instance.onReadVehicleInfoButtonClicked();
        executor.run();

        assertEquals("12345678901234567890", instance.getVin());

        verify(comparisonModule).reset();
        verify(comparisonModule).getVin();
        verify(comparisonModule).getCalibrationsAsString();
        verify(comparisonModule).compareFileToVehicle(any(ResultsListener.class), eq(reportFileModule), eq(2), eq(6));

        InOrder inOrder = inOrder(view);
        inOrder.verify(view).setVin("");
        inOrder.verify(view).setEngineCals("");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(false);
        inOrder.verify(view).setStopButtonEnabled(false);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(false);
        inOrder.verify(view).setAdapterComboBoxEnabled(false);
        inOrder.verify(view).setSelectFileButtonEnabled(false);
        inOrder.verify(view).setProgressBarValue(0, 6, 1);
        inOrder.verify(view).setProgressBarText("Reading Vehicle Identification Number");
        inOrder.verify(view).setVin("12345678901234567890");
        inOrder.verify(view).setProgressBarValue(0, 6, 2);
        inOrder.verify(view).setProgressBarText("Reading Vehicle Calibrations");
        inOrder.verify(view).setEngineCals("Engine Cals");
        inOrder.verify(view).setProgressBarText("Push Generate Vehicle Data Plate Button");
        inOrder.verify(view).setGenerateDataPlateButtonEnabled(true);
        inOrder.verify(view).setStopButtonEnabled(true);
        inOrder.verify(view).setReadVehicleInfoButtonEnabled(true);
        inOrder.verify(view).setAdapterComboBoxEnabled(true);
        inOrder.verify(view).setSelectFileButtonEnabled(true);
    }

    @Test
    public void testOnSelectFileButtonClicked() {
        instance.onSelectFileButtonClicked();
        verify(view).displayFileChooser();
    }

    @Test
    public void testOnStatusViewClosed() {
        instance.onStatusViewClosed();
        verify(monitorCompletionController).endTracking();
    }

    @Test
    public void testOnStopButtonClicked() throws Exception {
        Controller controller = mock(Controller.class);
        when(controller.isActive()).thenReturn(true);

        instance.setActiveController(controller);
        instance.onStopButtonClicked();

        verify(controller).stop();
        verify(view).setStatusViewVisible(false);
    }

    @Test
    public void testOnStopButtonClickedWithNoController() throws Exception {
        instance.onStopButtonClicked();
        // Nothing (bad) happens
    }

    @Test
    public void testOnStopButtonClickedWithStoppedController() throws Exception {
        Controller controller = mock(Controller.class);
        when(controller.isActive()).thenReturn(false);

        instance.setActiveController(controller);
        instance.onStopButtonClicked();

        verify(controller, never()).stop();
    }

    @Test
    public void testSetActiveController() throws Exception {
        Controller controller1 = mock(Controller.class);
        when(controller1.isActive()).thenReturn(true);
        instance.setActiveController(controller1);
        assertSame(controller1, instance.getActiveController());

        Controller controller2 = mock(Controller.class);
        instance.setActiveController(controller2);

        assertSame(controller2, instance.getActiveController());
        verify(controller1).stop();
    }

    @Test
    public void testSetActiveControllerNoController() throws Exception {
        Controller controller1 = mock(Controller.class);
        instance.setActiveController(controller1);
        assertSame(controller1, instance.getActiveController());
    }

    @Test
    public void testSetActiveControllerStoppedController() throws Exception {
        Controller controller1 = mock(Controller.class);
        when(controller1.isActive()).thenReturn(false);
        instance.setActiveController(controller1);
        assertSame(controller1, instance.getActiveController());

        Controller controller2 = mock(Controller.class);
        instance.setActiveController(controller2);
        assertSame(controller2, instance.getActiveController());

        verify(controller1, never()).stop();
    }

    @SuppressFBWarnings(value = "RU_INVOKE_RUN", justification = "Run is correct here for testing")
    @Test
    public void testShutdownHook() {
        assertEquals("Shutdown Hook Thread", shutdownHook.getName());
        shutdownHook.run();
        verify(reportFileModule).onProgramExit();
    }

}
