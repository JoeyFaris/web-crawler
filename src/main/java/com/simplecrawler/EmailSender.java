package com.simplecrawler;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.List;
import java.util.Properties;

public class EmailSender {
    public static final String RECIPIENT_EMAIL = System.getenv("RECIPIENT_EMAIL");
    public static final String SENDER_EMAIL = System.getenv("SENDER_EMAIL");
    public static final String SENDER_PASSWORD = System.getenv("SENDER_PASSWORD");

    public void scheduleEmailSending(String emailContent) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("emailContent", emailContent);

        JobDetail job = JobBuilder.newJob(EmailJob.class)
                .withIdentity("emailJob", "group1")
                .usingJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("emailTrigger", "group1")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 7 ? * MON-FRI"))
                .build();

        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();
        scheduler.scheduleJob(job, trigger);
    }

    public static class EmailJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            String emailContent = dataMap.getString("emailContent");
            sendEmail(emailContent);
        }
    }

    public static void sendEmail(String emailContent) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(RECIPIENT_EMAIL));
            message.setSubject("Daily Email");
            message.setText(emailContent);

            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendJobLinks(List<String> jobLinks, String recipientEmail) throws MessagingException {

        String host = "smtp.gmail.com";
        String username = SENDER_EMAIL;
        String password = SENDER_PASSWORD;
        int port = 587;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        }); 

        System.out.println("Session " + session);

        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email cannot be null or empty");
        }

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        
        StringBuilder content = new StringBuilder();
        content.append("Here are the job links:\n\n");
        for (String link : jobLinks) {
            content.append("Link: ").append(link).append("\n\n");
        }

        message.setText(content.toString());

        Transport.send(message);

        System.out.println("Email sent successfully to " + recipientEmail);
    }
}