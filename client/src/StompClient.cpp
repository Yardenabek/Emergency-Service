
#include "../include/StompClient.h"
#include <queue>
#include <vector>
#include <iostream>

// בנאי: מאתחל את מנהל החיבור, פרוטוקול STOMP, ומצב החיבור
StompClient::StompClient(const std::string &host, short port)
    : connectionHandler(host, port), protocol(), isConnected(false) {}

// הרסן: מוודא ניתוק מסודר אם האובייקט מושמד
StompClient::~StompClient() {
    if (isConnected) {
        disconnect();
    }
}

// התחברות לשרת
bool StompClient::connect(const std::string &username, const std::string &password) {
    if (!connectionHandler.connect()) { // ניסיון לפתוח חיבור TCP
        std::cerr << "Failed to connect to server" << std::endl;
        return false;
    }

    // יצירת מסגרת CONNECT עם שם משתמש וסיסמה
    std::string connectFrame = protocol.createConnectFrame(username, password);

    // שליחת המסגרת לשרת
    sendFrame(connectFrame);

    // עדכון הסטטוס והדפסת הודעה
    std::cout << "Connecting to server..." << std::endl;

    return true; // החיבור יושלם כאשר `readFromSocket` יזהה את CONNECTED
}

// ניתוק מהשרת
void StompClient::disconnect() {
    if (isConnected) {
        // יצירת מזהה ייחודי למסגרת DISCONNECT
        std::string receiptId = protocol.generateReceiptId();
        std::string disconnectFrame = protocol.createDisconnectFrame(receiptId);

        // שליחת המסגרת לשרת
        sendFrame(disconnectFrame);

        // חוט `readFromSocket` יטפל בתשובת השרת (מסגרת RECEIPT)
        std::cout << "Disconnecting from server..." << std::endl;

        connectionHandler.close(); // סגירת החיבור הפיזי
        isConnected = false;
        std::cout << "Disconnected from server!" << std::endl;
    }
}

// שליחת מסגרת לשרת
void StompClient::sendFrame(const std::string &frame) {
    if (!connectionHandler.sendFrameAscii(frame, '\n')) { // ניסיון לשלוח את המסגרת
        std::cerr << "Failed to send frame: " << frame << std::endl;
    }
}

// קבלת מסגרת מהשרת
std::string StompClient::receiveFrame() {
    std::string frame;
    if (!connectionHandler.getFrameAscii(frame, '\n')) { // קריאת מסגרת מה-Socket
        std::cerr << "Failed to receive frame" << std::endl;
    }
    return frame; // החזרת המסגרת שנקראה
}

// שליחת הודעה לערוץ
void StompClient::sendMessage(const std::string &channel, const std::string &message) {
    // יצירת מזהה ייחודי למסגרת SEND
    std::string receiptId = protocol.generateReceiptId();
    std::string frame = protocol.createSendFrame(channel, message, receiptId);

    // שליחת המסגרת לשרת
    sendFrame(frame);

    // שמירת המזהה בתור לצורך מעקב
    pendingReceipts.push(receiptId);
    std::cout << "Message sent to channel: " << channel << ", waiting for receipt..." << std::endl;
}

// רישום לערוץ
void StompClient::subscribe(const std::string &channel, int subscriptionId) {
    // יצירת מסגרת SUBSCRIBE
    std::string subscribeFrame = protocol.createSubscribeFrame(channel, subscriptionId);

    // שליחת המסגרת לשרת
    sendFrame(subscribeFrame);
    std::cout << "Subscribed to channel: " << channel << " with subscription ID: " << subscriptionId << std::endl;
}

// יציאה מערוץ
void StompClient::unsubscribe(int subscriptionId) {
    // יצירת מסגרת UNSUBSCRIBE
    std::string unsubscribeFrame = protocol.createUnsubscribeFrame(subscriptionId);

    // שליחת המסגרת לשרת
    sendFrame(unsubscribeFrame);
    std::cout << "Unsubscribed from subscription ID: " << subscriptionId << std::endl;
}

// קריאת פקודות מהמשתמש
void StompClient::readFromKeyboard() {
    std::string command;
    while (std::getline(std::cin, command)) { // קריאת פקודה מהמשתמש
        std::vector<std::string> parts; // יצירת וקטור לאחסון הפקודה והפרמטרים
        splitInput(command, ' ', parts); // שימוש בפונקציה המעודכנת

        if (parts.empty()) {
            continue;
        }

        const std::string &action = parts[0];

        if (action == "exit") { // פקודת יציאה
            disconnect();
            break;
        } else if (action == "connect" && parts.size() == 3) { // connect {username} {password}
            const std::string &username = parts[1];
            const std::string &password = parts[2];
            connect(username, password);
        } else if (action == "send" && parts.size() > 2) { // send {channel} {message}
            const std::string &channel = parts[1];
            const std::string message = command.substr(command.find(' ', command.find(' ') + 1) + 1);
            sendMessage(channel, message);
        } else if (action == "subscribe" && parts.size() == 3) { // subscribe {channel} {subscriptionId}
            const std::string &channel = parts[1];
            int subscriptionId = std::stoi(parts[2]);
            subscribe(channel, subscriptionId);
        } else if (action == "unsubscribe" && parts.size() == 2) { // unsubscribe {subscriptionId}
            int subscriptionId = std::stoi(parts[1]);
            unsubscribe(subscriptionId);
        } else {
            std::cerr << "Unknown or invalid command: " << command << std::endl;
        }
    }
}


// קריאת מסגרות מהשרת
void StompClient::readFromSocket() {
    std::string response;
    while ((response = receiveFrame()) != "") { // קריאת מסגרת מה-Socket
        if (protocol.isConnectedFrame(response)) { // מסגרת CONNECTED
            isConnected = true;
            std::cout << "Connected to server!" << std::endl;
        } else if (!pendingReceipts.empty() && protocol.isReceiptFrame(response, pendingReceipts.front())) { // מסגרת RECEIPT
            std::cout << "Server acknowledged receipt: " << pendingReceipts.front() << std::endl;
            pendingReceipts.pop(); // הסרת המזהה מהתור
        } else if (protocol.isMessageFrame(response)) { // מסגרת MESSAGE
            Event event(response); // יצירת אובייקט Event מהמסגרת
            std::cout << "New event received: " << event.get_name()
                      << ", City: " << event.get_city()
                      << ", Description: " << event.get_description() << std::endl;
        } else if (protocol.isErrorFrame(response)) { // מסגרת ERROR
            std::cerr << "Error from server: " << response << std::endl;

            // טיפול נוסף בשגיאה (אופציונלי)
            if (response.find("critical") != std::string::npos) { // זיהוי שגיאה חמורה
                std::cerr << "Critical error received. Stopping client..." << std::endl;
                disconnect();
                break;
            }
        } else { // מסגרת לא מזוהה
            std::cerr << "Unexpected frame in readFromSocket: " << response << std::endl;
        }
    }
}



#include <thread>
#include <mutex>
#include <atomic>
#include <string>

std::atomic<bool> keepRunning(true); // דגל לעצירת התוכנית בצורה מסודרת

void userInputThread(ConnectionHandler &connectionHandler) {
    // טיפול בקלט מהמשתמש ושליחה לשרת
    while (keepRunning) {
        std::string input;
        std::getline(std::cin, input); // קלט מהמשתמש

        if (input == "logout") {
            // שליחת פקודת DISCONNECT לשרת
            connectionHandler.sendLine("DISCONNECT\n");
            keepRunning = false; // מסיים את התוכנית
            break;
        }

        if (!connectionHandler.sendLine(input)) {
            std::cerr << "Failed to send message to server. Exiting user input thread..." << std::endl;
            keepRunning = false;
            break;
        }
    }
}

void serverResponseThread(ConnectionHandler &connectionHandler) {
    // טיפול בתגובות שמתקבלות מהשרת
    while (keepRunning) {
        std::string response;
        if (!connectionHandler.getLine(response)) {
            std::cerr << "Disconnected from server. Exiting server response thread..." << std::endl;
            keepRunning = false;
            break;
        }

        std::cout << "Server Response: " << response << std::endl;

        // עצירה במקרה של תגובת "bye" מהשרת
        if (response == "bye") {
            std::cout << "Server requested to exit. Stopping client..." << std::endl;
            keepRunning = false;
            break;
        }
    }
}

int main(int argc, char *argv[]) {
    // בדיקת פרמטרים
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl;
        return -1;
    }

    std::string host = argv[1];
    short port = atoi(argv[2]);

    // יצירת אובייקט ConnectionHandler
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    std::cout << "Connected to " << host << ":" << port << std::endl;

    // יצירת threads לטיפול בקלט ובתגובות
    std::thread inputThread(userInputThread, std::ref(connectionHandler));
    std::thread responseThread(serverResponseThread, std::ref(connectionHandler));

    // המתנה לסיום threads
    inputThread.join();
    responseThread.join();

    std::cout << "Client stopped. Exiting program." << std::endl;
    return 0;
}
