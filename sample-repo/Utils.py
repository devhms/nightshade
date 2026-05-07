def calculate_tax(amount, rate):
    # Standard tax calculation
    tax = amount * rate
    total = amount + tax
    return total

def _internal_processing(data_list):
    processed = []
    for item in data_list:
        val1 = item * 2
        val2 = val1 + 5
        processed.append(val2)
    return processed

class ConfigLoader:
    def __init__(self, path):
        self.config_path = path
        self.loaded = False
        
    def load(self):
        print(f"Loading from {self.config_path}")
        self.loaded = True
