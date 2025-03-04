#ifndef KEYBOARD_INPUT_H
#define KEYBOARD_INPUT_H

#include <string>
#include <vector>

// פונקציה לפיצול מחרוזת למרכיבים לפי תו מפריד
void splitInput(const std::string &input, char delimiter, std::vector<std::string> &output);

#endif // KEYBOARD_INPUT_H
