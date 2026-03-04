#!/usr/bin/env python3
import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from app.database import get_signal_stats, get_signal_credibility

def debug_signal_stats():
    print("🔍 Debugging /signals/stats endpoint...")
    try:
        stats = get_signal_stats()
        print(f"✅ get_signal_stats() successful:")
        print(f"   signals_7d: {stats.get('signals_7d')}")
        print(f"   evidence_7d: {stats.get('evidence_7d')}")
        return True
    except Exception as e:
        print(f"❌ get_signal_stats() failed:")
        print(f"   Error: {e}")
        import traceback
        traceback.print_exc()
        return False

def debug_signal_credibility():
    print("\n🔍 Debugging /insights/credibility endpoint...")
    try:
        credibility_7d = get_signal_credibility(7)
        print(f"✅ get_signal_credibility(7) successful:")
        print(f"   signals_total: {credibility_7d.get('signals_total')}")
        print(f"   signals_with_evidence: {credibility_7d.get('signals_with_evidence')}")
        
        credibility_30d = get_signal_credibility(30)
        print(f"✅ get_signal_credibility(30) successful:")
        print(f"   signals_total: {credibility_30d.get('signals_total')}")
        return True
    except Exception as e:
        print(f"❌ get_signal_credibility() failed:")
        print(f"   Error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("Starting endpoint debugging...")
    
    success1 = debug_signal_stats()
    success2 = debug_signal_credibility()
    
    if success1 and success2:
        print("\n🎉 All endpoints working correctly!")
        sys.exit(0)
    else:
        print("\n❌ Some endpoints have issues")
        sys.exit(1)