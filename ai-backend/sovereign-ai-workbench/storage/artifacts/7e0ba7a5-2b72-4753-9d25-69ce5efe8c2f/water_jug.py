from collections import deque

def water_jug_bfs(cap1, cap2, target):
    """
    Solve the classic water‑jug problem using breadth‑first search.
    cap1, cap2 – capacities of the two jugs (litres)
    target     – desired amount of water (litres)
    Returns a list of ( jug1_amount, jug2_amount ) states from start to goal,
    or None if the target cannot be reached.
    """
    start = (0, 0)
    queue = deque([(start, [start])])
    visited = {start}

    while queue:
        (a, b), path = queue.popleft()

        # Goal test: target reached in either jug or total
        if a == target or b == target or a + b == target:
            return path

        # All possible next states
        successors = [
            (cap1, b),                     # Fill jug 1
            (a, cap2),                     # Fill jug 2
            (0, b),                        # Empty jug 1
            (a, 0),                        # Empty jug 2
            (a - min(a, cap2 - b), b + min(a, cap2 - b)),  # Pour 1→2
            (a + min(b, cap1 - a), b - min(b, cap1 - a))   # Pour 2→1
        ]

        for ns in successors:
            if ns not in visited:
                visited.add(ns)
                queue.append((ns, path + [ns]))

    return None


if __name__ == "__main__":
    # Example: 3‑L jug, 5‑L jug, target 4 L
    path = water_jug_bfs(3, 5, 4)
    if path:
        for i, (a, b) in enumerate(path):
            print(f"Step {i}: ({a}, {b})")
    else:
        print("No solution found")
