package chubbiocertificatemanagement.customalert;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(includeFieldNames = false)
public class CertificateMetadata {

	@Setter
	@Getter
	private String applicationName = "NA";

	@Setter
	@Getter
	private String serviceName = "NA";

	@Setter
	@Getter
	private String certificateName = "NA";

	@Setter
	@Getter
	private Date expiryDate = null;

	@Setter
	@Getter
	private String sanDNS = "NA";

	@Setter
	@Getter
	private String environment = "NA";

	@Setter
	@Getter
	private String owner = "NA";
	
	@Setter
	@Getter
	private String assignmentGroup = "NA";
	
	@Setter
	@Getter
	private String comment = "NA";
}