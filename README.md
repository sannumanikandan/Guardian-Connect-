 Guardian Connect

An IoT-Based Health Monitoring and Emergency Assistance System

 Project Overview

Guardian Connect is a project that combines an Android application with Arduino-based hardware to support heart-rate monitoring and emergency assistance.

The project integrates Bluetooth communication, heart-rate data monitoring, and emergency contact features into a single system.

 Key Features

Heart-Rate Monitoring:
Receives heart-rate readings from compatible Arduino hardware over Bluetooth.
- Android Application: Provides a dashboard for viewing heart-rate data.
- Bluetooth Connectivity: Communicates with a paired HC-05 Bluetooth module.
- SOS Assistance: Supports sending emergency SMS messages to a saved contact.
- Emergency Calling: Provides an option to open the phone dialer.
- Emergency Contact Management: Allows users to save and update an emergency contact.

 Technologies Used

- Android
- Java
- XML
- Arduino
- HC-05 Bluetooth Module
- Embedded Systems

Repository Structure


Guardian-Connect/
├── app/
│   └── src/
│       └── main/
├── arduino/
├── .gitignore
├── README.md
├── SETUP_GUIDE.md
└── app_build_gradle_reference.txt


 Hardware Requirements

- Arduino-compatible board
- HC-05 Bluetooth module
- Compatible heart-rate sensor or simulator setup
- Android smartphone

Software Requirements

- Android development environment
- Java and XML
- Arduino IDE

 Getting Started

1. Clone or download this repository.
2. Review `SETUP_GUIDE.md` for setup instructions.
3. Open the Android project in a compatible Android development environment.
4. Upload the appropriate Arduino sketch using the Arduino IDE.
5. Pair the HC-05 module with your Android phone.
6. Test Bluetooth communication and the application's available features.

 Android Permissions

The application declares permissions related to Bluetooth connectivity, SMS messaging, and calling. Grant the necessary permissions on your device before testing the corresponding features.

 Disclaimer

This project is intended for educational and demonstration purposes. It is not a certified medical device and should not be used as a substitute for professional medical care or emergency services.

 Project Objectives

- Explore IoT-based health monitoring.
- Integrate embedded hardware with an Android application.
- Implement Bluetooth serial communication.
- Demonstrate emergency assistance features.

 Future Enhancements

- Improve the user interface and accessibility.
- Expand health-data monitoring capabilities.
- Improve Bluetooth connection reliability.
- Add additional emergency notification options.

---

**Guardian Connect — Connecting Health Monitoring with Emergency Assistance.**
