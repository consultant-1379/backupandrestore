import os
import sys

bro_agent_path = os.path.dirname(os.path.abspath(__file__))

sys.path.append(os.path.join(bro_agent_path, "generated"))
