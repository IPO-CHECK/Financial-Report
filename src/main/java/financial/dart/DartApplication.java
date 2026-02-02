package financial.dart;
import financial.dart.config.DartProperties;
import financial.dart.vector.properties.OpenAiProperties;
import financial.dart.vector.properties.QdrantProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(DartProperties.class)
@ConfigurationPropertiesScan(basePackageClasses = {QdrantProperties.class, OpenAiProperties.class})
public class DartApplication {

	public static void main(String[] args) {
		SpringApplication.run(DartApplication.class, args);
	}

}
