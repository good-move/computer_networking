class PermutationGenerator:
    def __init__(self, max_length: int, alphabet: str):
        self.max_length = max_length
        self.start_index = 0
        self.alphabet = alphabet
        self.max_index = self.__count_max_perm_index()

    def get_string_for_index(self, index) -> str:
        if index == 0:
            return ""

        index -= 1
        perm_length = 1
        start = 0
        perms_count = len(self.alphabet)
        while start + perms_count <= index:
            start += perms_count
            perms_count *= len(self.alphabet)
            perm_length += 1

        permutation = ""
        range_length = 1 << (2 * (perm_length - 1))
        while perm_length > 0:
            p = index - start
            k = int(p / range_length)
            start = start + k * range_length
            permutation += self.alphabet[k]
            range_length /= len(self.alphabet)
            perm_length -= 1

        return permutation

    def get_index_for_string(self, string: str) -> int:
        length = len(string)
        index = 0
        perms_count = 1

        for i in range(length):
            index += perms_count
            perms_count *= len(self.alphabet)

        i = 0
        while length > 0:
            perms_count /= len(self.alphabet)
            index += perms_count * self.alphabet.find(string[i])
            length -= 1
            i += 1

        return int(index)

    def get_next_range(self, range_size: int, range_start: str = None) -> tuple:
        if range_start is None:
            start_index = self.start_index
            range_start = self.get_string_for_index(start_index)
        else:
            start_index = self.get_index_for_string(range_start)

        if start_index > self.max_index:
            return ()

        end_index = min(start_index + range_size - 1, self.max_index)
        self.start_index = end_index + 1
        range_end = self.get_string_for_index(end_index)

        return range_start, range_end

    def __count_max_perm_index(self) -> int:
        max_perm_index = 0
        length = 0
        perms_count = 1
        while length <= self.max_length:
            max_perm_index += perms_count
            perms_count *= len(self.alphabet)
            length += 1

        return max_perm_index - 1
