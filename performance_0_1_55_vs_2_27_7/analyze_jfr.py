#!/usr/bin/env python3
"""
Automated JFR Analysis Tool
Analyzes and compares JFR profiling data from old and new JSch versions
"""

import re
import sys
from collections import defaultdict, Counter
from typing import Dict, List, Tuple

def parse_jfr_samples(filename: str) -> Dict[str, int]:
    """Parse JFR execution samples and count stack frames"""
    method_samples = Counter()

    try:
        with open(filename, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()

            # Look for stack trace patterns in JFR output
            # Format: jdk.ExecutionSample {
            #   ...
            #   stackTrace = [
            #     com.jcraft.jsch.Session.run() line: 123
            #     ...
            #   ]
            # }

            # Extract method names from stack traces
            method_pattern = r'([\w.$]+\.[\w<>]+)\([^)]*\)'
            matches = re.findall(method_pattern, content)

            for method in matches:
                # Clean up method name
                method = method.strip()
                if method:
                    method_samples[method] += 1

    except FileNotFoundError:
        print(f"Error: Could not find file {filename}")
        return {}
    except Exception as e:
        print(f"Error parsing {filename}: {e}")
        return {}

    return dict(method_samples)

def parse_jfr_summary(filename: str) -> Dict[str, str]:
    """Parse JFR summary file for metadata"""
    metadata = {}

    try:
        with open(filename, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()

            # Extract key metrics
            duration_match = re.search(r'Duration:\s+([\d.]+\s+\w+)', content)
            if duration_match:
                metadata['duration'] = duration_match.group(1)

            samples_match = re.search(r'ExecutionSample.*?(\d+)', content, re.DOTALL)
            if samples_match:
                metadata['total_samples'] = samples_match.group(1)

    except FileNotFoundError:
        print(f"Warning: Could not find summary file {filename}")
    except Exception as e:
        print(f"Warning: Error parsing summary {filename}: {e}")

    return metadata

def get_top_methods(method_samples: Dict[str, int], n: int = 20) -> List[Tuple[str, int]]:
    """Get top N methods by sample count"""
    return sorted(method_samples.items(), key=lambda x: x[1], reverse=True)[:n]

def compare_profiles(old_samples: Dict[str, int], new_samples: Dict[str, int], top_n: int = 30):
    """Compare two profiles and identify differences"""

    print("\n" + "="*80)
    print("JFR PROFILE COMPARISON: OLD vs NEW JSch")
    print("="*80)

    old_total = sum(old_samples.values())
    new_total = sum(new_samples.values())

    print(f"\nTotal Samples:")
    print(f"  Old JSch: {old_total:,}")
    print(f"  New JSch: {new_total:,}")

    # Get top methods from both
    old_top = get_top_methods(old_samples, top_n)
    new_top = get_top_methods(new_samples, top_n)

    # Create normalized percentages
    old_pct = {method: (count / old_total * 100) if old_total > 0 else 0
               for method, count in old_samples.items()}
    new_pct = {method: (count / new_total * 100) if new_total > 0 else 0
               for method, count in new_samples.items()}

    # Find all methods in either profile
    all_methods = set(old_samples.keys()) | set(new_samples.keys())

    # Calculate differences
    differences = []
    for method in all_methods:
        old_p = old_pct.get(method, 0.0)
        new_p = new_pct.get(method, 0.0)
        diff = new_p - old_p

        # Only include methods with significant presence or change
        if new_p > 1.0 or old_p > 1.0 or abs(diff) > 0.5:
            differences.append((method, old_p, new_p, diff))

    # Sort by absolute difference
    differences.sort(key=lambda x: abs(x[3]), reverse=True)

    print("\n" + "-"*80)
    print("TOP METHODS BY SAMPLE COUNT")
    print("-"*80)
    print(f"\n{'Method':<60} {'Old %':>8} {'New %':>8} {'Diff':>8}")
    print("-"*80)

    for method, old_p, new_p, diff in differences[:25]:
        # Shorten method name if too long
        short_method = method if len(method) <= 58 else method[:55] + "..."
        diff_str = f"{diff:+.2f}%" if abs(diff) >= 0.01 else "~0%"
        print(f"{short_method:<60} {old_p:7.2f}% {new_p:7.2f}% {diff_str:>8}")

    # Identify performance hotspots (methods taking >2% in new version)
    print("\n" + "="*80)
    print("PERFORMANCE HOTSPOTS (>2% in NEW JSch)")
    print("="*80)

    hotspots = [(m, o, n, d) for m, o, n, d in differences if n > 2.0]
    hotspots.sort(key=lambda x: x[2], reverse=True)

    if hotspots:
        for method, old_p, new_p, diff in hotspots[:15]:
            short_method = method if len(method) <= 58 else method[:55] + "..."
            print(f"\n{short_method}")
            print(f"  Old: {old_p:6.2f}%  New: {new_p:6.2f}%  Change: {diff:+.2f}%")
    else:
        print("No significant hotspots found (>2% CPU time)")

    # Identify biggest regressions
    print("\n" + "="*80)
    print("BIGGEST REGRESSIONS (methods slower in NEW)")
    print("="*80)

    regressions = [(m, o, n, d) for m, o, n, d in differences if d > 0.5]
    regressions.sort(key=lambda x: x[3], reverse=True)

    if regressions:
        for method, old_p, new_p, diff in regressions[:15]:
            short_method = method if len(method) <= 58 else method[:55] + "..."
            print(f"\n{short_method}")
            print(f"  Old: {old_p:6.2f}%  New: {new_p:6.2f}%  Regression: +{diff:.2f}%")
    else:
        print("No significant regressions found")

    # Identify improvements
    print("\n" + "="*80)
    print("IMPROVEMENTS (methods faster in NEW)")
    print("="*80)

    improvements = [(m, o, n, d) for m, o, n, d in differences if d < -0.5]
    improvements.sort(key=lambda x: x[3])

    if improvements:
        for method, old_p, new_p, diff in improvements[:15]:
            short_method = method if len(method) <= 58 else method[:55] + "..."
            print(f"\n{short_method}")
            print(f"  Old: {old_p:6.2f}%  New: {new_p:6.2f}%  Improvement: {diff:.2f}%")
    else:
        print("No significant improvements found")

    # JSch-specific analysis
    print("\n" + "="*80)
    print("JSCH-SPECIFIC METHOD ANALYSIS")
    print("="*80)

    jsch_methods = [(m, o, n, d) for m, o, n, d in differences
                    if 'jsch' in m.lower() and (n > 0.5 or o > 0.5)]
    jsch_methods.sort(key=lambda x: x[2], reverse=True)

    if jsch_methods:
        print(f"\n{'Method':<60} {'Old %':>8} {'New %':>8} {'Diff':>8}")
        print("-"*80)
        for method, old_p, new_p, diff in jsch_methods[:20]:
            short_method = method if len(method) <= 58 else method[:55] + "..."
            diff_str = f"{diff:+.2f}%" if abs(diff) >= 0.01 else "~0%"
            print(f"{short_method:<60} {old_p:7.2f}% {new_p:7.2f}% {diff_str:>8}")
    else:
        print("No JSch-specific methods found")

    print("\n" + "="*80)
    print("ANALYSIS COMPLETE")
    print("="*80)
    print("\nKey findings:")
    print(f"  - Analyzed {len(differences)} unique methods")
    print(f"  - Found {len(regressions)} methods with regressions")
    print(f"  - Found {len(improvements)} methods with improvements")
    print(f"  - Identified {len(hotspots)} performance hotspots in new version")
    print("\nFor detailed flame graph view, open .jfr files in JDK Mission Control")

def main():
    """Main analysis function"""

    print("JFR Profile Analyzer for JSch Performance Investigation")
    print("="*80)

    # Parse old JSch profile
    print("\nParsing OLD JSch profile...")
    old_samples = parse_jfr_samples("jfr_old_jsch_samples.txt")
    old_meta = parse_jfr_summary("jfr_old_jsch_summary.txt")

    if not old_samples:
        print("ERROR: Could not parse old JSch samples")
        print("Make sure to run profile_old_jsch.sh first")
        return 1

    print(f"  Loaded {len(old_samples)} unique methods from old JSch")
    if old_meta:
        print(f"  Duration: {old_meta.get('duration', 'unknown')}")

    # Parse new JSch profile
    print("\nParsing NEW JSch profile...")
    new_samples = parse_jfr_samples("jfr_new_jsch_samples.txt")
    new_meta = parse_jfr_summary("jfr_new_jsch_summary.txt")

    if not new_samples:
        print("ERROR: Could not parse new JSch samples")
        print("Make sure to run profile_new_jsch.sh first")
        return 1

    print(f"  Loaded {len(new_samples)} unique methods from new JSch")
    if new_meta:
        print(f"  Duration: {new_meta.get('duration', 'unknown')}")

    # Compare profiles
    compare_profiles(old_samples, new_samples)

    return 0

if __name__ == "__main__":
    sys.exit(main())
