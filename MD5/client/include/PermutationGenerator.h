//
// Created by alexey on 05.11.17.
//

#ifndef MD5_PERMUTATIONGENERATOR_H
#define MD5_PERMUTATIONGENERATOR_H

#include <string>
#include <vector>

class PermutationGenerator {

  public:
    PermutationGenerator(const std::vector<char> alphabet);
    /*
     * Sets iteration range bounds.
     *
     * @args
     *  rangeStart (string)- string, at which brute force attack starts
     *  rangeEnd (string) - string, at which brute force attack must stop
     */
    void SetRange(const std::string& rangeStart, const std::string& rangeEnd);

    /*
     * Sets iteration range start.
     *
     * @args
     *  rangeStart (string) - string, at which iteration starts
     */
    void SetRangeStart(const std::string rangeStart);

    /*
     * Sets iteration range end.
     *
     * @args
     *  rangeStart (string) - string, at which iteration starts
     */
    void SetRangeEnd(const std::string rangeEnd);

    /*
     * Maps a number from the Natural numbers set to alphabet permutation
     */
    std::string GetStringForIndex(size_t index);

    /*
     * Maps an alphabet permutation of arbitrary length to a number from the
     * Natural numbers set
     */
    size_t GetIndexForString(const std::string& str);


    /*
     * Calculates permutation string length, positioned at index `index`
     */
    size_t GetStringLengthForIndex(size_t index);

    /*
     * Iterator-like methods
     */
    bool HasNext();
    std::string& GetNext();
    std::string& GetCurrent();

  private:

    std::vector<char> alphabet;
    std::string currentPermutation;
    size_t currentPosition;
    size_t rangeStart;
    size_t rangeEnd;

    size_t GetLetterIndex(const char& letter);
};


#endif //MD5_PERMUTATIONGENERATOR_H
