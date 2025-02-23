#include "../include/StompProtocol.h"
#include <sstream>
#include <atomic>

// משתנה סטטי ליצירת מזהים ייחודיים
static std::atomic<int> receiptCounter{0};

// בנאי
StompProtocol::StompProtocol() {}

std::string StompProtocol::createConnectFrame(const std::string &username, const std::string &password) {
    std::ostringstream frame;
    frame << "CONNECT\n"
          << "accept-version:1.2\n"
          << "login:" << username << "\n"
          << "passcode:" << password << "\n\n";
    return frame.str();
}

std::string StompProtocol::createDisconnectFrame(const std::string &receiptId) {
    std::ostringstream frame;
    frame << "DISCONNECT\n"
          << "receipt:" << receiptId << "\n\n";
    return frame.str();
}

std::string StompProtocol::createSendFrame(const std::string &channel, const std::string &message, const std::string &receiptId) {
    std::ostringstream frame;
    frame << "SEND\n"
          << "destination:" << channel << "\n"
          << "receipt:" << receiptId << "\n\n"
          << message << "\n";
    return frame.str();
}

std::string StompProtocol::createSubscribeFrame(const std::string &channel, int subscriptionId) {
    std::ostringstream frame;
    frame << "SUBSCRIBE\n"
          << "destination:" << channel << "\n"
          << "id:" << subscriptionId << "\n"
          << "ack:auto\n\n";
    return frame.str();
}

std::string StompProtocol::createUnsubscribeFrame(int subscriptionId) {
    std::ostringstream frame;
    frame << "UNSUBSCRIBE\n"
          << "id:" << subscriptionId << "\n\n";
    return frame.str();
}

bool StompProtocol::isConnectedFrame(const std::string &frame) {
    return frame.find("CONNECTED") == 0;
}

bool StompProtocol::isReceiptFrame(const std::string &frame, const std::string &receiptId) {
    return frame.find("RECEIPT") == 0 && frame.find("receipt-id:" + receiptId) != std::string::npos;
}

bool StompProtocol::isMessageFrame(const std::string &frame) {
    return frame.find("MESSAGE") == 0;
}

bool StompProtocol::isErrorFrame(const std::string &frame) {
    return frame.find("ERROR") == 0;
}

std::string StompProtocol::generateReceiptId() {
    return "receipt-" + std::to_string(receiptCounter.fetch_add(1));
}
