//
// Created by alexey on 05.11.17.
//

#ifndef MD5_MD5CRACKER_H
#define MD5_MD5CRACKER_H

#include <openssl/md5.h>
#include <stdexcept>
#include <iostream>
#include <string>

#include "PermutationGenerator.h"


/*
 * Interface definition for all MD5 crackers.
 */
class Md5Cracker {

  public:
    Md5Cracker(PermutationGenerator* permGen, const std::string& hash);
    virtual ~Md5Cracker();

    /*
     * Sets brute force range.
     *
     * @args
     *  rangeStart (string)- string, at which brute force attack starts
     *  rangeEnd (string) - string, at which brute force attack must stop
     */
    void SetRange(const std::string& rangeStart, const std::string& rangeEnd);
    bool MatchFound() const;
    std::string GetHashOrigin() const;

    /*
     * Check all possible permutations using `alphabet`, which fall in the preset range
     * and checks each of them for md5 match
     */
    void Crack();

    /*
     * Tests whether argument string md5 hash matches target hash (the one to crack)
     */
    bool TestHashMatch(const std::string& testString) const;

private:

    PermutationGenerator* permGen_ = nullptr;
    std::string rangeStart;
    std::string rangeEnd;

    std::string hashPrototype_;
    std::string targetHash_;
    bool matchFound_;

};


#endif //MD5_MD5CRACKER_H
