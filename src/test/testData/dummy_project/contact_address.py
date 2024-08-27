class Address:
    def __init__(self, street, city, postal_code):
        self.street = street
        self.city = city
        self.postal_code = postal_code

    def __str__(self):
        return f"{self.street}, {self.city}, {self.postal_code}"

class Person:
    def __init__(self, name, age, address):
        self.name = name
        self.age = age
        self.address = address

    def __str__(self):
        return f"Name: {self.name}, Age: {self.age}, Address: {self.address}"

address1 = Address("Sample Street 123", "Sample City", "12345")
person1 = Person("John Doe", 30, address1)

print(person1)
