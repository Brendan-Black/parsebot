package black.parsebot.writer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import black.parsebot.config.SftpConfig;
import black.parsebot.event.Event;
import black.parsebot.event.EventBus;
import black.parsebot.event.EventSeverity;
import black.parsebot.event.EventType;
import black.parsebot.model.TransformedData;

public class SftpWriter {

	private static final Logger log = LoggerFactory.getLogger(SftpWriter.class);

	private final SftpConfig config;
	private final EventBus eventBus;

	public SftpWriter(SftpConfig config, EventBus eventBus) {
		this.config = config;
		this.eventBus = eventBus;
	}

	/**
	 * Uploads every file, or throws: the caller only marks a message processed once its output has been delivered.
	 */
	public void write(List<TransformedData> dataList) throws IOException {
		if (config.getHost().isBlank()) {
			throw new IOException("SFTP host not configured, so " + dataList.size() + " output file(s) have nowhere to go");
		}

		Session session = null;
		ChannelSftp channel = null;

		try {
			JSch jsch = new JSch();

			String privateKey = config.getPrivateKey();
			if (!privateKey.isBlank()) {
				jsch.addIdentity(privateKey);
			}

			session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());

			if (privateKey.isBlank()) {
				session.setPassword(config.getPassword());
			}

			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30_000);

			channel = (ChannelSftp) session.openChannel(config.getProtocol());
			channel.connect(30_000);
			channel.cd(config.getRemoteDirectory());

			for (TransformedData data : dataList) {
				log.info("Uploading: {} to {}", data.getFilename(), config.getRemoteDirectory());
				channel.put(new ByteArrayInputStream(data.getContent()), data.getFilename());
			}

			log.info("Uploaded {} file(s)", dataList.size());
		} catch (Exception e) {
			log.error("SFTP upload failed", e);
			eventBus.publish(Event.create(
				EventType.SFTP_WRITE_FAILED,
				EventSeverity.AUDIT,
				"SFTP upload failed: " + e.getMessage(),
				Map.of(
					"host", config.getHost(),
					"remoteDirectory", config.getRemoteDirectory(),
					"fileCount", String.valueOf(dataList.size()),
					"error", String.valueOf(e.getMessage())
				)));
			throw new IOException("SFTP upload of " + dataList.size() + " file(s) to " + config.getHost() + ":" + config.getRemoteDirectory() + " failed", e);
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
