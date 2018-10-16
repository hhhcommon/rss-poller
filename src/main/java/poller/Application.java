package poller;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.rometools.rome.feed.synd.SyndEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class Application implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);


  @Override
  public void run(String... args) {
    ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("integration/rss-integration-context.xml");
    PollableChannel feedChannel = ac.getBean("feedChannel", PollableChannel.class);
        Message<SyndEntry> message = (Message<SyndEntry>) feedChannel.receive(1000);
        while (message != null) {
          SyndEntry entry = message.getPayload();
          System.out.println(entry.getPublishedDate() + " - " + entry.getTitle());
          saveTempFile(entry.getDescription().getValue(), entry.getTitle());
          message = (Message<SyndEntry>) feedChannel.receive(1000);
        }

        convertFilesToPdf();
        cleanup();
    ac.close();
  }

  /**
   *
   */
  private void convertFilesToPdf() {
    try {
      Files.walk(Paths.get("/tmp/out")).forEach(p -> {
        try {
          generatePDFFromHTML(p.toString());
        } catch ( Exception e ) {
          System.out.println(e.getMessage());
        }
      });
    } catch ( IOException e ) {
      System.out.println(e.getMessage());
    }

  }


  /**
   * Temporary function designed to remove the metadata-store.properties file that Spring Integration Feed generates.
   */
  private void cleanup()  {
    try {
      Files.deleteIfExists(Paths.get("../../../metadata-store.properties"));
    } catch ( IOException e ) {
      System.out.println(e.getMessage());
    }
  }

  /**
   *
   * @param content
   * @param entryTitle
   */
  private void saveTempFile(String content, String entryTitle)  {
    try {
      FileOutputStream fileStream = new FileOutputStream("/tmp/out/" + entryTitle + ".html");
      fileStream.write(content.getBytes());
    } catch (IOException e ) {
      System.out.println(e.getMessage());
    }
  }

  /**
   *
   * @param filename
   * @throws IOException
   * @throws DocumentException
   * @throws FileNotFoundException
   */
  private static void generatePDFFromHTML(String filename) throws IOException, DocumentException, FileNotFoundException {
    Document document = new Document();
    PdfWriter writer = PdfWriter.getInstance(document,
      new FileOutputStream(filename + ".pdf"));
    document.open();
    XMLWorkerHelper.getInstance().parseXHtml(writer, document,
      new FileInputStream(filename));
    document.close();
  }



  public static void main(String[] args) throws Exception {
    LOGGER.info("Application.main() started.");
    // Start up Spring Boot
    // NOTE:  See ApplicationListener.applicationReady() for post-startup checks
    SpringApplication.run(Application.class, args);
    LOGGER.info("Poller is up.");
  }
}
