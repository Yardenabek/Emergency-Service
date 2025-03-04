#include "../include/keyboardInput.h"
#include <sstream>

// מימוש הפונקציה splitInput
void splitInput(const std::string &input, char delimiter, std::vector<std::string> &output) {
    std::stringstream ss(input);
    std::string token;
    while (std::getline(ss, token, delimiter)) {
        if (!token.empty()) {
            output.push_back(token);
        }
    }
}
