package black.parsebot.writer;

import black.parsebot.config.AppConfig;
import black.parsebot.model.TransformedData;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.List;

public class SftpWriter {

    private static final Logger log = LoggerFactory.getLogger(SftpWriter.class);

    private final AppConfig config;

    public SftpWriter(AppConfig config) {
        this.config = config;
    }

    public void write(List<TransformedData> dataList) {
        if (config.getSftpHost().isBlank()) {
            log.warn("SFTP host not configured, skipping upload");
            return;
        }

        Session session = null;
        ChannelSftp channel = null;

        try {
            JSch jsch = new JSch();

            String privateKey = config.getSftpPrivateKey();
            if (!privateKey.isBlank()) {
                jsch.addIdentity(privateKey);
            }

            session = jsch.getSession(config.getSftpUsername(), config.getSftpHost(), config.getSftpPort());

            if (privateKey.isBlank()) {
                session.setPassword(config.getSftpPassword());
            }

            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30_000);

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(30_000);
            channel.cd(config.getSftpRemoteDirectory());

            for (TransformedData data : dataList) {
                log.info("Uploading: {} to {}", data.getFilename(), config.getSftpRemoteDirectory());
                channel.put(new ByteArrayInputStream(data.getContent()), data.getFilename());
            }

            log.info("Uploaded {} file(s)", dataList.size());
        } catch (Exception e) {
            log.error("SFTP upload failed", e);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
