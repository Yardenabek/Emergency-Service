#ifndef STOMPCLIENT_H
#define STOMPCLIENT_H

//#include "ConnectionHandler.h" // לניהול ה-Socket
#include "StompProtocol.h" // מחלקת פרוטוקול STOMP
#include <string>
#include <queue> // לניהול תור של receipt
#include "event.h"
class StompClient {
private:
    ConnectionHandler connectionHandler; // לניהול ה-Socket
    StompProtocol protocol; // ניהול מסגרות STOMP
    bool isConnected; // מצב החיבור לשרת
    std::queue<std::string> pendingReceipts; // תור של מזהי receipt לפעולות כמו send ו-disconnect

public:
    StompClient(const std::string &host, short port); // בנאי
    ~StompClient(); // הרסן

    // חיבור וניתוק לשרת
    bool connect(const std::string &username, const std::string &password); // התחברות לשרת
    void disconnect(); // ניתוק מהשרת

    // שליחה וקבלה של מסגרות STOMP
    void sendFrame(const std::string &frame); // שליחת מסגרת STOMP
    std::string receiveFrame(); // קבלת מסגרת STOMP

    // פעולות על ערוצים
    void sendMessage(const std::string &channel, const std::string &message); // שליחת הודעה לערוץ
    void subscribe(const std::string &channel, int subscriptionId); // רישום לערוץ
    void unsubscribe(int subscriptionId); // יציאה מערוץ

    // קריאת קלט מהמשתמש ומה-Socket
    void readFromKeyboard(); // קריאת פקודות מהמשתמש
    void readFromSocket(); // קריאת הודעות מהשרת
};

#endif // STOMPCLIENT_H
