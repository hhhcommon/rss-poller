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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

@SpringBootApplication
public class Application implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(Application.class);


  @Override
  public void run(String... args) {
    ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("spring/integration/rss-integration-context.xml");
    PollableChannel feedChannel = ac.getBean("feedChannel", PollableChannel.class);
        Message<SyndEntry> message = (Message<SyndEntry>) feedChannel.receive(1000);
        while (message != null) {
          SyndEntry entry = message.getPayload();
          System.out.println(entry.getPublishedDate() + " - " + entry.getTitle());
//          System.out.println(entry.toString());
          writeToDisk(entry.getDescription().getValue(), entry.getTitle());
          message = (Message<SyndEntry>) feedChannel.receive(1000);
        }
    ac.close();
  }

  /**
   *
   * @param content
   * @param entryTitle
   */
  private void writeToDisk(String content, String entryTitle)  {
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
      new FileOutputStream("src/output/html.pdf"));
    document.open();
    XMLWorkerHelper.getInstance().parseXHtml(writer, document,
      new FileInputStream(filename));
    document.close();
  }



  public static void main(String[] args) throws Exception {
    logger.debug("Application.main() started.");
    // Start up Spring Boot
    // NOTE:  See ApplicationListener.applicationReady() for post-startup checks
    SpringApplication.run(Application.class, args);
    logger.debug("Poller is up.");
  }
}
