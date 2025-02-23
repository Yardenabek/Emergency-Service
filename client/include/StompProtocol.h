
#ifndef STOMPPROTOCOL_H
#define STOMPPROTOCOL_H

#include <string>
#include "../include/ConnectionHandler.h"

class StompProtocol {
public:
    StompProtocol(); // בנאי

    // פונקציות ליצירת מסגרות STOMP
    std::string createConnectFrame(const std::string &username, const std::string &password);
    std::string createSendFrame(const std::string &channel, const std::string &message, const std::string &receiptId);
    std::string createSubscribeFrame(const std::string &channel, int subscriptionId);
    std::string createUnsubscribeFrame(int subscriptionId);
    std::string createDisconnectFrame(const std::string &receiptId);

    // פונקציות לזיהוי סוגי מסגרות
    bool isConnectedFrame(const std::string &frame);
    bool isReceiptFrame(const std::string &frame, const std::string &receiptId);
    bool isMessageFrame(const std::string &frame);
    bool isErrorFrame(const std::string &frame);

    // פונקציה ליצירת מזהים ייחודיים
    std::string generateReceiptId();
};

#endif // STOMPPROTOCOL_H
