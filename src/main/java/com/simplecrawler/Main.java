package com.simplecrawler;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("https://en.wikipedia.org/wiki/Percolation_theory");
            return;
        }

        String url = args[0];
        SimpleCrawler crawler = new SimpleCrawler();
        crawler.crawl(url);
    }
}