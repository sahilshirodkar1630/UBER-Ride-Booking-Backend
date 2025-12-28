package com.sahil.project.uber.uberApp;

import com.sahil.project.uber.uberApp.services.EmailSenderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UberAppApplicationTests {

	@Autowired
	private EmailSenderService emailSenderService;

	@Test
	void contextLoads() {
		emailSenderService.sendEmail(
				"yiwox37258@nctime.com",
				"This is the Testing Email",
				"Body of my email sahil"
		);
	}

	@Test
	void sendMultipleEmails(){
		String emails[] ={
				"mvyfi48938@healthforwomen.info",
				"sahilshirodkar3005@gmail.com"
		};

		emailSenderService.sendEmail(
				emails,
				"This is the Testing Multiple Email",
				"Body of UBER Demo"
		);
	}

}
