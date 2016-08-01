package ee.openeid.siva.sample.ci.info;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ee.openeid.siva.sample.configuration.BuildInfoProperties;
import ee.openeid.siva.sample.test.utils.TestFileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FilesystemBuildInfoFileLoaderTest {
    private FilesystemBuildInfoFileLoader loader = new FilesystemBuildInfoFileLoader();

    @Mock
    private Appender<ILoggingEvent> mockAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;

    @Before
    public void setUp() throws Exception {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.addAppender(mockAppender);
    }

    @After
    public void tearDown() throws Exception {
        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logger.detachAppender(mockAppender);
    }

    @Test
    public void givenValidInfoFileWillReturnCorrectBuildInfo() throws Exception {
        loader.setProperties(createBuildProperties("/test-info.yml"));
        Observable<BuildInfo> rxBuildInfo = loader.loadBuildInfo();
        BuildInfo buildInfo = rxBuildInfo.toBlocking().first();

        assertThat(buildInfo.getTravisCi().getBuildNumber()).isEqualTo("#106");
        assertThat(buildInfo.getGithub().getAuthorName()).isEqualTo("Andres Voll");
        assertThat(buildInfo.getGithub().getShortHash()).isEqualTo("dfce8a94");
        assertThat(buildInfo.getGithub().getUrl()).contains("dfce8a94c54b8d94153807b153baaf56bfd9317e");
        assertThat(buildInfo.getTravisCi().getBuildUrl()).contains("135787867");
    }

    @Test
    public void givenInvalidInfoFilePathWillReturnEmptyBuildInfo() throws Exception {
        BuildInfoProperties properties = new BuildInfoProperties();
        properties.setInfoFile("wrong.info-file.yml");

        loader.setProperties(properties);
        Observable<BuildInfo> rxBuildInfo = loader.loadBuildInfo();
        BuildInfo buildInfo = rxBuildInfo.toBlocking().first();


        assertThat(buildInfo.getGithub()).isNull();
        assertThat(buildInfo.getTravisCi()).isNull();
    }

    @Test
    public void givenInfoFileWithMissingGithubInfoWillReturnBuildInfo() throws Exception {
        loader.setProperties(createBuildProperties("/info-missing-github.yml"));
        Observable<BuildInfo> rxBuildInfo = loader.loadBuildInfo();
        BuildInfo buildInfo = rxBuildInfo.toBlocking().first();

        assertThat(buildInfo.getGithub()).isNull();
        assertThat(buildInfo.getTravisCi().getBuildNumber()).isEqualTo("#106");
    }

    @Test
    public void givenInvalidWindowsBuildInfoFilePathWillReturnEmptyBuildInfo() throws Exception {
        BuildInfoProperties properties = new BuildInfoProperties();
        properties.setInfoFile("C:\\invalid-build-info-path.yml");

        loader.setProperties(properties);
        Observable<BuildInfo> buildInfo = loader.loadBuildInfo();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());
        final List<LoggingEvent> loggingEvent = captorLoggingEvent.getAllValues();

        assertThat(loggingEvent.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(loggingEvent.get(0).getFormattedMessage()).contains("No such file exists: C:\\invalid-build-info-path.yml");
        assertThat(buildInfo.toBlocking().first().getGithub()).isNull();
    }

    private static BuildInfoProperties createBuildProperties(String filePath) {
        File file = TestFileUtils.loadTestFile(filePath);

        BuildInfoProperties properties = new BuildInfoProperties();
        properties.setInfoFile(file.getAbsolutePath());
        return properties;
    }
}