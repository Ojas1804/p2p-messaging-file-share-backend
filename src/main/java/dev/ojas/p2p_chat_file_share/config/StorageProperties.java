package dev.ojas.p2p_chat_file_share.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "p2pcf.storage")
public class StorageProperties {
    private String dir;
    private String userDetailsFileName;
}
