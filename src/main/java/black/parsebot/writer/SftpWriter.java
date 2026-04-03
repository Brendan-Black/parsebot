package black.parsebot.writer;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import black.parsebot.config.AppConfig.SftpConfig;
import black.parsebot.model.TransformedData;

public class SftpWriter {

	private static final Logger log = LoggerFactory.getLogger(SftpWriter.class);

	private final SftpConfig config;

	public SftpWriter(SftpConfig config) {
		this.config = config;
	}

	public void write(List<TransformedData> dataList) {
		if (config.getHost().isBlank()) {
			log.warn("SFTP host not configured, skipping upload");
			return;
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
