package chubbiocertificatemanagement.customalert;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class CustomAlert {

	@Value("${source.excel.file}")
	private String sourceExcelFile;

	@Value("${from.email.address}")
	private String fromEmailAddress;

	@Value("${from.email.name}")
	private String fromEmailName;

	@Value("${from.email.password}")
	private String fromEmailPassword;

	@Value("${to.email.address}")
	private String toEmailAddress;

	@Value("${source.excel.file.field.to.variable.mapping}")
	private String fieldToVariableMapping;
	
	@Value("${certificate.date.validation.days}")
	private String certificateDataValidationDays;

	private TrustManager[] trustAllCerts = null;

	private HashMap<Integer, String> columnToVariable = new HashMap<Integer, String>();

	public CustomAlert() {
		trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void start() {
		InputStream is = CustomAlert.class.getClassLoader().getResourceAsStream(sourceExcelFile);
		StringBuffer message = new StringBuffer();
		message.append("Following certificate are going to expire in " + certificateDataValidationDays + " days, please take immediate action -<br /><br />");
		message.append(fetchCertificate(getHostNamesAndOtherDetailsFROMFile(is)));
		sendEmailAlert("Certificate Exipry Alert", message.toString());
	}

	private String fetchCertificate(ArrayList<CertificateMetadata> certificateMetadataList) {
		StringBuffer buffer = new StringBuffer();
		for (CertificateMetadata certificate : certificateMetadataList) {
			try {
				UrlValidator urlValidator = new UrlValidator();
				if (urlValidator.isValid("https://" + certificate.getCertificateName().trim())) {
					URL url = new URL("https://" + certificate.getCertificateName().trim());
					HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
					System.out.println(conn.getResponseCode());
					Certificate[] certs = conn.getServerCertificates();
					for (Certificate cert : certs) {
						X509Certificate xcert = (X509Certificate) cert;
						//System.out.println(certificate);
						//System.out.println("Type : " + xcert.getType());
						//System.out.println("SubjectX500Principal : " + xcert.getSubjectX500Principal());
						//System.out.println("SubjectAlternativeNames() : " + xcert.getSubjectAlternativeNames());
						Date certificateExpiryDate = xcert.getNotAfter();
						long diffInMillies = Math.abs(certificateExpiryDate.getTime() - new Date().getTime());
					    long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
						if (diff <= Integer.parseInt(certificateDataValidationDays)) {
							buffer.append(
								"Application: " + certificate.getApplicationName() + ", " + 
								"Service: " + certificate.getServiceName() + ", " + 
								"Environment: " + certificate.getEnvironment() + ", " +
								"Certificate: " + certificate.getCertificateName() + ", " +
								"Expiry Date: " + certificate.getExpiryDate() + ", " +
								"SAN DNS: " + ((xcert.getSubjectAlternativeNames() != null)? xcert.getSubjectAlternativeNames(): null) + ", " +
								"Assignment Group: " + certificate.getAssignmentGroup() + "<br />");
						}
					}
					conn.disconnect();
				} else {
					System.out.println("Invalid---->https://" + certificate.getCertificateName());
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} 
		}
		return buffer.toString();
	}

	private ArrayList<CertificateMetadata> getHostNamesAndOtherDetailsFROMFile(InputStream is) {
		try {
			Workbook workbook = new XSSFWorkbook(is);
			Sheet sheet = workbook.getSheetAt(4);
			ArrayList<CertificateMetadata> certificateMetadataList = new ArrayList<CertificateMetadata>();
			for (Row row : sheet) {
				CertificateMetadata certificateMetadata = null;
				for (Cell cell : row) {
					if (row.getRowNum() == 0) {
						parseFieldToVariableMapping(row);
					} else {
						if (certificateMetadata == null)
							certificateMetadata = new CertificateMetadata();
						String methodName = columnToVariable.get(cell.getColumnIndex());
						try {
							switch (cell.getCellType()) {
							case STRING:
								if (methodName != null) {
									Method method = certificateMetadata.getClass().getMethod(methodName, String.class);
									method.invoke(certificateMetadata,
											(cell.getStringCellValue() instanceof String) ? cell.getStringCellValue()
													: null);
								}
								break;
							case NUMERIC:
								if (methodName != null) {
									if (DateUtil.isCellDateFormatted(cell)) {
										Method method = certificateMetadata.getClass().getMethod(methodName, Date.class);
										method.invoke(certificateMetadata,
												(cell.getDateCellValue() instanceof Date) ? cell.getDateCellValue()
														: null);
									} else {
										Method method = certificateMetadata.getClass().getMethod(methodName, String.class);
										method.invoke(certificateMetadata,
												(Double.toString(cell.getNumericCellValue()) instanceof String)
														? Double.toString(cell.getNumericCellValue())
														: null);
									}
								}
								break;
							case BOOLEAN:
								if (methodName != null) {
									Method method = certificateMetadata.getClass().getMethod(methodName, String.class);
									method.invoke(certificateMetadata,
											(Boolean.toString(cell.getBooleanCellValue()) instanceof String)
													? Boolean.toString(cell.getBooleanCellValue())
													: null);
								}
								break;
							case FORMULA:
								break;
							default:
								break;
							}
						} catch (Exception e) {
							// TODO
						}
					}
				}
				if (certificateMetadata != null)
					certificateMetadataList.add(certificateMetadata);
			}
			workbook.close();
			return certificateMetadataList;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void parseFieldToVariableMapping(Row row) {
		String[] splitData = fieldToVariableMapping.split(";");
		HashMap<String, String> temp = new HashMap<String, String>();
		for (String eachData : splitData) {
			String[] keyValue = eachData.split(":");
			temp.put(keyValue[0], keyValue[1]);
		}
		for (Cell cell : row) {
			if (row.getRowNum() == 0)
				switch (cell.getCellType()) {
				case STRING: {
					String header = cell.getStringCellValue();
					int index = cell.getColumnIndex();
					if (temp.get(header) != null)
						columnToVariable.put(index, temp.get(header));
					break;
				}
				default:
					break;
				}
			else
				break;
		}
	}

	private void sendEmailAlert(String subject, String body) {
		try {
			Properties props = new Properties();
			props.put("mail.smtp.host", "intsmtp.chubb.com"); // SMTP Host
			props.put("mail.smtp.port", "25"); // TLS Port
			props.put("mail.smtp.auth", "true"); // enable authentication
			props.put("mail.smtp.starttls.enable", "true"); // enable STARTTLS

			// create Authenticator object to pass in Session.getInstance argument
			Authenticator auth = new Authenticator() {
				// override the getPasswordAuthentication method
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(fromEmailAddress, fromEmailPassword);
				}
			};
			Session session = Session.getInstance(props, auth);

			MimeMessage msg = new MimeMessage(session);

			// set message headers
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			// msg.addHeader("Content-Transfer-Encoding", "8bit");
			msg.setFrom(new InternetAddress(fromEmailAddress, fromEmailName));
			msg.setReplyTo(InternetAddress.parse(fromEmailAddress, false));
			msg.setSubject(subject, "UTF-8");
			msg.setHeader("X-Priority", "1");
			// msg.setText(body, "UTF-8");
			msg.setContent(body, "text/html");
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmailAddress, false));

			Transport.send(msg);
			// consoleOutput("EMail Sent Successfully!!");
		} catch (Exception e) {
			// consoleError(e.getMessage());
		}
	}
}