#!/usr/bin/env python3
import json
import random
import os

# Lists for random generation
COMPANY_NAMES = ["TechCorp", "DataSystems", "CloudWorks", "InnovateLabs", "SmartTech",
                 "FutureNet", "GlobalTech", "NextGen", "CoreSystems", "QuantumLabs"]
CITIES = ["Milano", "Roma", "Torino", "Firenze", "Napoli", "Bologna", "Venezia",
          "Genova", "Verona", "Palermo"]
COUNTRIES = ["Italia", "France", "Germany", "Spain", "UK"]
STREETS = ["Via Roma", "Corso Italia", "Via Garibaldi", "Piazza Centrale", "Viale Europa"]
DEPT_NAMES = ["Engineering", "Research", "Operations", "Analytics", "Development",
              "Infrastructure", "Security", "Platform", "Innovation"]
TEAM_NAMES = ["Alpha", "Beta", "Gamma", "Delta", "Omega", "Sigma", "Phoenix", "Titan"]
PROJECT_NAMES = ["ProjectX", "Initiative", "Platform", "System", "Framework", "Engine"]
STATUSES = ["in_progress", "planning", "completed", "testing"]
TECHNOLOGIES = ["Python", "Java", "Go", "Rust", "C++", "JavaScript", "TypeScript",
                "Scala", "Kotlin", "Ruby", "PHP", "C#", "Swift", "R", "Julia"]
PERSON_NAMES = ["Mario", "Laura", "Giuseppe", "Anna", "Francesco", "Elena", "Giovanni",
                "Sofia", "Alessandro", "Giulia"]
SURNAMES = ["Rossi", "Bianchi", "Verdi", "Russo", "Ferrari", "Esposito", "Colombo"]

def generate_person():
    name = f"{random.choice(PERSON_NAMES)} {random.choice(SURNAMES)}"
    email = name.lower().replace(" ", ".") + "@company.it"
    return {"name": name, "email": email}

def generate_project(proj_id):
    return {
        "project_id": f"P{proj_id:05d}",
        "name": f"{random.choice(PROJECT_NAMES)}{random.randint(1, 999)}",
        "status": random.choice(STATUSES),
        "budget": random.randint(10000, 1000000),
        "start_date": f"2024-{random.randint(1, 12):02d}-{random.randint(1, 28):02d}",
        "technologies": random.sample(TECHNOLOGIES, random.randint(2, 5)),
        "milestones": [
            {
                "id": f"M{i}",
                "name": f"Milestone {i}",
                "status": random.choice(STATUSES),
                "completion": random.randint(0, 100)
            } for i in range(random.randint(2, 5))
        ]
    }

def generate_team(team_id, num_projects):
    return {
        "team_id": f"T{team_id:05d}",
        "team_name": f"{random.choice(TEAM_NAMES)} Team",
        "size": random.randint(3, 15),
        "lead": generate_person(),
        "projects": [generate_project(team_id * 1000 + i) for i in range(num_projects)]
    }

def generate_department(dept_id, num_teams, num_projects_per_team):
    return {
        "id": f"D{dept_id:05d}",
        "name": random.choice(DEPT_NAMES),
        "budget": random.randint(100000, 5000000),
        "manager": generate_person(),
        "teams": [generate_team(dept_id * 100 + i, num_projects_per_team)
                  for i in range(num_teams)]
    }

def generate_company(comp_id, num_depts, num_teams_per_dept, num_projects_per_team):
    return {
        "company": {
            "name": f"{random.choice(COMPANY_NAMES)} {comp_id}",
            "founded": random.randint(2000, 2023),
            "revenue": random.randint(1000000, 100000000),
            "employees": random.randint(50, 10000),
            "headquarters": {
                "city": random.choice(CITIES),
                "country": random.choice(COUNTRIES),
                "address": {
                    "street": random.choice(STREETS),
                    "number": random.randint(1, 999),
                    "postal_code": f"{random.randint(10000, 99999)}"
                }
            },
            "departments": [generate_department(comp_id * 10 + i, num_teams_per_dept,
                                                 num_projects_per_team)
                            for i in range(num_depts)]
        }
    }

def generate_data(target_size_mb=100):
    data = {"companies": []}
    current_size_mb = 0
    comp_id = 0
    num_depts = 5
    num_teams = 4
    num_projects = 4

    while current_size_mb < target_size_mb:
        company = generate_company(comp_id, num_depts, num_teams, num_projects)
        data["companies"].append(company)
        comp_id += 1

        if comp_id % 10 == 0:
            json_str = json.dumps(data)
            current_size_mb = len(json_str) / (1024 * 1024)

    return json.dumps(data, indent=2)

script_dir = os.path.dirname(os.path.abspath(__file__))
output_file = os.path.join(script_dir, "large_data.json")
data_json = generate_data(2)

with open(output_file, "w") as f:
    f.write(data_json)
