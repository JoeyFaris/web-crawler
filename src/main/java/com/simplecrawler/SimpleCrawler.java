package com.simplecrawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;   
import javax.mail.MessagingException;
import io.github.cdimascio.dotenv.Dotenv;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.util.TimeZone;
    
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class SimpleCrawler {
    private static final int MAX_JOBS = 10;
    private static final int MAX_EXPERIENCE = 3;
    private static final String[] JOB_TITLES = {"front end engineer", "software engineer", "web developer"};

    public static void main(String[] args) throws SchedulerException {
        // Create a new scheduler
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

        // Define the job and tie it to our CrawlerJob class
        JobDetail job = newJob(CrawlerJob.class)
            .withIdentity("crawlerJob", "group1")
            .build();

        // Create a cron trigger that runs at 6:00 AM PST every weekday
        Trigger trigger = newTrigger()
            .withIdentity("crawlerTrigger", "group1")
            .withSchedule(cronSchedule("0 0 6 ? * MON-FRI")
                .inTimeZone(TimeZone.getTimeZone("America/Los_Angeles")))
            .build();

        // Schedule the job
        scheduler.scheduleJob(job, trigger);

        // Start the scheduler
        scheduler.start();
    }

    public static class CrawlerJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            List<String> jobLinks = new ArrayList<>();
            Set<String> companies = new HashSet<>();

            for (String jobTitle : JOB_TITLES) {
                try {
                    String url = "https://www.linkedin.com/jobs/search/?keywords=" + jobTitle.replace(" ", "%20");
                    Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(10000)
                        .get();
                    
                    Elements jobCards = doc.select(".jobs-search__results-list > li");

                    for (Element card : jobCards) {
                        if (jobLinks.size() >= MAX_JOBS) break;

                        Element titleElement = card.selectFirst(".base-search-card__title");
                        Element linkElement = card.selectFirst("a.base-card__full-link");
                        Element descriptionElement = card.selectFirst(".base-search-card__metadata");
                        Element companyElement = card.selectFirst(".base-search-card__subtitle");

                        if (titleElement != null && linkElement != null && descriptionElement != null && companyElement != null) {
                            String title = titleElement.text();
                            String link = linkElement.attr("href");
                            String description = descriptionElement.text().toLowerCase();
                            String company = companyElement.text().trim();

                            if (isRelevantJob(title, description) && !companies.contains(company)) {
                                jobLinks.add(link);
                                companies.add(company);
                            }
                        }
                    }

                    // Add a delay between requests
                    TimeUnit.SECONDS.sleep(5);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Error crawling for " + jobTitle + ": " + e.getMessage());
                }
            }

            System.out.println("Found " + jobLinks.size() + " relevant job postings:");
            for (String link : jobLinks) {
                System.out.println(link);
            }

            Dotenv dotenv = Dotenv.load();

            String recipientEmail = dotenv.get("RECIPIENT_EMAIL");
            if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
                System.err.println("RECIPIENT_EMAIL environment variable is not set or is empty");
                return; 
            }
            try {
                EmailSender.sendJobLinks(jobLinks, recipientEmail);
            } catch (MessagingException e) {
                System.err.println("Error sending email: " + e.getMessage());
            }
        }
    }

    private static boolean isRelevantJob(String title, String description) {
        String lowerTitle = title.toLowerCase();
        for (String jobTitle : JOB_TITLES) {
            if (lowerTitle.contains(jobTitle)) {
                return !hasMoreThanMaxExperience(description);
            }
        }
        return false;
    }

    private static boolean hasMoreThanMaxExperience(String description) {
        String[] words = description.split("\\s+");
        for (int i = 0; i < words.length - 2; i++) {
            if (words[i].matches("\\d+") && words[i + 1].startsWith("year")) {
                int years = Integer.parseInt(words[i]);
                if (years > MAX_EXPERIENCE) {
                    return true;
                }
            }
        }
        return false;
    }
}