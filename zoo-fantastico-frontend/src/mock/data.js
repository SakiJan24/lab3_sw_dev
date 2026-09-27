export const INITIAL_ZONES = [
  {
    id: 1,
    name: "Santuario del Maletín",
    description: "Espacio mágico dimensional expandido con hábitats adaptados para especies dóciles y delicadas.",
    capacity: 5
  },
  {
    id: 2,
    name: "Reserva Tropical Nundú",
    description: "Zona de clima cálido y selva espesa con hechizos contenedores de aliento tóxico.",
    capacity: 3
  },
  {
    id: 3,
    name: "Cúpula de Aves del Viento",
    description: "Gran domo con corrientes térmicas mágicas ideal para criaturas aladas de gran envergadura.",
    capacity: 4
  },
  {
    id: 4,
    name: "Túneles del Escarbato",
    description: "Bóveda subterránea reforzada con encantamientos anti-hurto para amantes de lo brillante.",
    capacity: 8
  }
];

export const INITIAL_CREATURES = [
  {
    id: 101,
    name: "Teddy",
    species: "Escarbato (Niffler)",
    size: 0.35,
    dangerLevel: 2,
    healthStatus: "healthy",
    zoneId: 4
  },
  {
    id: 102,
    name: "Frank",
    species: "Ave del Trueno (Thunderbird)",
    size: 4.8,
    dangerLevel: 8,
    healthStatus: "stable",
    zoneId: 3
  },
  {
    id: 103,
    name: "Pickett",
    species: "Bowtruckle",
    size: 0.20,
    dangerLevel: 1,
    healthStatus: "healthy",
    zoneId: 1
  },
  {
    id: 104,
    name: "Dougal",
    species: "Demiguise",
    size: 1.10,
    dangerLevel: 3,
    healthStatus: "recovering",
    zoneId: 1
  },
  {
    id: 105,
    name: "Venom-Wing",
    species: "Caño Desvanecedor (Swooping Evil)",
    size: 2.40,
    dangerLevel: 9,
    healthStatus: "critical",
    zoneId: 2
  },
  {
    id: 106,
    name: "Nessi",
    species: "Kelpie",
    size: 3.50,
    dangerLevel: 7,
    healthStatus: "healthy",
    zoneId: 2
  }
];

