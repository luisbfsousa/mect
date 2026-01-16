import matplotlib.pyplot as plt
import subprocess
import sys
import re
import numpy as np
from matplotlib.ticker import MaxNLocator

def get_coin_counts(source):
    counts = []
    pattern = re.compile(r"COINS_FOUND:\s*(\d+)")
    
    if isinstance(source, list):
        iterator = source
    else:
        try:
            with open(source, 'r') as f:
                iterator = f.readlines()
        except FileNotFoundError:
            print(f"Error: File {source} not found.")
            return []

    for line in iterator:
        match = pattern.search(line)
        if match:
            try:
                val = int(match.group(1))
                counts.append(val)
            except ValueError:
                continue
    return counts

def run_cuda_executable(exec_path, args=[]):
    cmd = [exec_path] + args
    print(f"Running: {' '.join(cmd)}")
    try:
        result = subprocess.run(
            cmd, 
            stdout=subprocess.PIPE, 
            stderr=subprocess.PIPE, 
            text=True,
            check=True
        )
        return result.stdout.splitlines()
    except subprocess.CalledProcessError as e:
        print(f"Error running executable: {e}")
        return []
    except FileNotFoundError:
        print(f"Executable not found at: {exec_path}")
        return []

def plot_histogram(counts, output_file="coins_found_histogram.png"):
    if not counts:
        print("No data found to plot.")
        return

    plt.figure(figsize=(10, 6))
    
    unique_counts = sorted(list(set(counts)))
    if not unique_counts:
        unique_counts = [0]
    
    bins = np.arange(min(counts) - 0.5, max(counts) + 1.5, 1)
    
    plt.hist(counts, bins=bins, color='orange', edgecolor='black', alpha=0.7, rwidth=0.8)
    
    plt.title(f'Distribution of Coins Found per Kernel Run\n(Total Batches: {len(counts)})')
    plt.xlabel('Number of Coins Found')
    plt.ylabel('Frequency (Number of Batches)')
    
    ax = plt.gca()
    ax.xaxis.set_major_locator(MaxNLocator(integer=True))
    
    total_coins = sum(counts)
    mean_coins = np.mean(counts)
    plt.text(0.95, 0.95, 
             f'Total Coins: {total_coins}\nMean per Batch: {mean_coins:.4f}\nMax per Batch: {max(counts)}', 
             transform=ax.transAxes, 
             verticalalignment='top', 
             horizontalalignment='right',
             bbox=dict(boxstyle='round', facecolor='white', alpha=0.5))

    plt.grid(axis='y', alpha=0.3)
    plt.savefig(output_file)
    print(f"Histogram saved to {output_file}")
    plt.close()

if __name__ == "__main__":
    RUN_BINARY = False
    EXECUTABLE_PATH = "./search_cuda"
    EXECUTABLE_ARGS = ["1000"]
    LOG_FILE_PATH = "output.log"

    data = []

    if len(sys.argv) > 1:
        data = get_coin_counts(sys.argv[1])
    elif RUN_BINARY:
        lines = run_cuda_executable(EXECUTABLE_PATH, EXECUTABLE_ARGS)
        data = get_coin_counts(lines)
    else:
        print(f"No arguments provided. Attempting to read from {LOG_FILE_PATH}...")
        data = get_coin_counts(LOG_FILE_PATH)
        if not data:
            print("\n[Demo Mode] No data found. Generating dummy Poisson-like data...")
            data = np.random.poisson(lam=0.01, size=5000).tolist()

    if data:
        print(f"Processed {len(data)} batches.")
        plot_histogram(data)
    else:
        print("Could not obtain data.")