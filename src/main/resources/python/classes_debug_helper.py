# classes_debug_helper.py
import inspect
import sys


def get_defined_classes():
    classes = []
    for name, obj in inspect.getmembers(sys.modules[__name__]):
        if inspect.isclass(obj):
            classes.append(name)
    return classes
