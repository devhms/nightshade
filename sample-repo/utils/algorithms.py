def bubble_sort(data):
    """Sort list using bubble sort algorithm."""
    # outer loop controls passes
    n = len(data)
    for i in range(n):
        swapped = False
        # inner loop performs comparisons
        for j in range(0, n - i - 1):
            if data[j] > data[j + 1]:
                # swap adjacent elements
                data[j], data[j + 1] = data[j + 1], data[j]
                swapped = True
        if not swapped:
            break
    return data


def binary_search(arr, target):
    """Find target in sorted array using binary search."""
    # initialize search boundaries
    left = 0
    right = len(arr) - 1
    while left <= right:
        mid = (left + right) // 2
        # check middle element
        if arr[mid] == target:
            return mid
        elif arr[mid] < target:
            left = mid + 1
        else:
            right = mid - 1
    return -1


def count_words(text):
    """Count word frequencies in a string."""
    # split text into words
    words = text.lower().split()
    frequency = {}
    for word in words:
        # increment count or initialize to 1
        if word in frequency:
            frequency[word] += 1
        else:
            frequency[word] = 1
    return frequency


class DataProcessor:
    """Processes a list of numeric data."""

    def __init__(self, data):
        # initialize processor with data list
        self.data = list(data)
        self.processed = False
        self.result = None

    def compute_mean(self):
        """Calculate arithmetic mean of data."""
        if not self.data:
            return 0.0
        total = sum(self.data)
        mean_value = total / len(self.data)
        self.result = mean_value
        return mean_value

    def compute_variance(self):
        """Calculate sample variance of data."""
        if len(self.data) < 2:
            return 0.0
        mean = self.compute_mean()
        variance = sum((x - mean) ** 2 for x in self.data) / (len(self.data) - 1)
        return variance
