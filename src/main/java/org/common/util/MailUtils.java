package org.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;


/**
 * 專案名稱: motcManagement<br>
 * 建立時間: 下午2:14:36<br>
 *
 * @author Van
 * @version 1.0
 */
public class MailUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MailUtils.class);
	
	public static final String SMTP = "smtp";
	public static final String SMTPS = "smtps";
	public static final String IMAP = "imap";
	public static final String IMAPS = "imaps";
	public static final String STYLE = "<style>* { font-family: 微軟正黑體;}</style>";
	public static final String DEFAULT_MIMETYPE = "text/html;charset=UTF-8";
	public static final String IMAP_HOST = "mail.imap.host";
	public static final String SMTP_HOST = "mail.smtp.host";
	public static final String MAIL_USER_NAME = "mail.username";
	public static final String MAIL_USER_AUTH = "mail.password";
	public static final String MAIL_SUBJECT = "mail.subject";
	public static final String MAIL_CONTENT = "mail.content";
	public static final String MAIL_FROM = "mail.from";
	public static final String MAIL_TO = "mail.to";
	public static final String MAIL_BACKUP_ENABLE = "mail.backup.enable";
	public static final String MAIL_BACKUP_FOLDER = "mail.backup.folder";
	private static final String ALIAS = "交通數據匯流平台";
	
	private static final String MAIL_RULE = "^\\w+((-\\w+)|(\\.\\w+))*\\@[A-Za-z0-9]+((\\.|-)[A-Za-z0-9]+)*\\.[A-Za-z]+$";

	private MailUtils() {
	}

	public static boolean sendHtmlMail(final Properties properties, CharSequence subject, CharSequence charSequence, List<File> files, String... toUsers) {
		final boolean backupEnable = Boolean.parseBoolean(properties.getProperty(MAIL_BACKUP_ENABLE));
		final String from = properties.getProperty(MAIL_FROM);
		int toLength = toUsers.length;
		List<InternetAddress> toAddresssList = new ArrayList<>(toLength);
		Session session = Session.getDefaultInstance(properties, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(properties.getProperty(MAIL_USER_NAME), properties.getProperty(MAIL_USER_AUTH));
			}
		});
		try {
			// Mail content and attachments
			Multipart multipart = new MimeMultipart();
			// Set mail content
			BodyPart bodyPart = new MimeBodyPart();
			bodyPart.setContent(STYLE + charSequence.toString(), DEFAULT_MIMETYPE);
			multipart.addBodyPart(bodyPart);
			// Set attachments
			files.forEach(file -> {
				try {
					MimeBodyPart mbp = new MimeBodyPart();
					FileDataSource fds = new FileDataSource(file);
					mbp.setDataHandler(new DataHandler(fds));
					mbp.setFileName(MimeUtility.encodeText(file.getName(), StandardCharsets.UTF_8.name(), "B"));
					multipart.addBodyPart(mbp);
				} catch (MessagingException | UnsupportedEncodingException e) {
					LOGGER.error(e.getMessage(), e);
				}
			});
			// Set send users
			Message message = new MimeMessage(session);
			for (int i = 0; i < toLength; i++) {
				if (toUsers[i].matches(MAIL_RULE)) toAddresssList.add(new InternetAddress(toUsers[i]));
			}

			message.setFrom(new InternetAddress(from, MimeUtility.encodeText(ALIAS, StandardCharsets.UTF_8.name(), "B")));
			message.setRecipients(Message.RecipientType.TO, toAddresssList.toArray(new InternetAddress[0]));
			message.setSubject(MimeUtility.encodeText(subject.toString(), StandardCharsets.UTF_8.name(), "B"));
			message.setSentDate(new Date());
			message.setContent(multipart);
			if (from.matches(MAIL_RULE)) Transport.send(message);

			return !backupEnable || backupMail(session, properties, message);
		} catch (UnsupportedEncodingException | MessagingException e) {
			LOGGER.error(e.getMessage(), e);
		}

		return false;
	}
	
	public static boolean sendHtmlMail(final Properties properties, CharSequence subject, CharSequence charSequence) {
		return sendHtmlMail(properties, subject, charSequence, new ArrayList<>(), properties.getProperty(MAIL_TO).split(","));
	}

	public static boolean sendHtmlMail(final Properties properties, List<File> attachments) {
		return sendHtmlMail(properties, properties.getProperty(MAIL_SUBJECT), properties.getProperty(MAIL_CONTENT), attachments, properties.getProperty(MAIL_TO).split(","));
	}
	
	public static boolean sendHtmlMail(final Properties properties, CharSequence subject, CharSequence content, String... toUsers) {
		return sendHtmlMail(properties, subject, content, new ArrayList<>(), toUsers);
	}
	
	public static boolean sendHtmlMail(final Properties properties, CharSequence subject, CharSequence content, List<File> files) {
		return sendHtmlMail(properties, subject, content, files, properties.getProperty(MAIL_TO).split(","));
	}
	
	private static boolean backupMail(final Session session, final Properties properties, final Message msg) throws MessagingException {
	    final Store store = session.getStore(IMAP);
	    store.connect(
	    		properties.getProperty(IMAP_HOST),
	    		properties.getProperty(MAIL_USER_NAME),
	    		properties.getProperty(MAIL_USER_AUTH)
	    );

	    final Folder folder = store.getFolder(properties.getProperty(MAIL_BACKUP_FOLDER));
	    if (!folder.exists()) folder.create(Folder.HOLDS_MESSAGES);
	    folder.open(Folder.READ_WRITE);
	    folder.appendMessages(new Message[] { msg });
	    
	    return true;
	}
}
