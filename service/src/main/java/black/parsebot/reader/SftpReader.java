package black.parsebot.reader;

import black.parsebot.config.AppConfig;
import black.parsebot.config.SftpConfig;
import black.parsebot.model.raw.RawSftpData;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SftpReader {

  private static final Logger log = LoggerFactory.getLogger(SftpReader.class);

  private final SftpConfig config;

  public SftpReader(AppConfig config) {
    this.config = config.getSftpConfig();
  }

  public List<RawSftpData> read() {
    List<RawSftpData> results = new ArrayList<>();

    if (config.getHost().isBlank()) {
      log.debug("SFTP host not configured, skipping SFTP read");
      return results;
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

      channel = (ChannelSftp) session.openChannel("sftp");
      channel.connect(30_000);
      channel.cd(config.getRemoteDirectory());

      Vector<ChannelSftp.LsEntry> entries = channel.ls(".");
      for (ChannelSftp.LsEntry entry : entries) {
        if (entry.getAttrs().isDir()) {
          continue;
        }

        String name = entry.getFilename();
        log.info("Reading SFTP file: {}", name);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        channel.get(name, out);
        results.add(new RawSftpData(name, out.toByteArray()));
      }

      log.info("Read {} file(s) from SFTP {}", results.size(), config.getRemoteDirectory());
    } catch (Exception e) {
      log.error("SFTP read failed", e);
    } finally {
      if (channel != null && channel.isConnected()) {
        channel.disconnect();
      }
      if (session != null && session.isConnected()) {
        session.disconnect();
      }
    }

    return results;
  }
}
