# Reward Service

The service manages five types of reward scores: Health, Fitness, Growth, Strength, and Power.

1. **Health Score:** Reflects a user's learning progress, increasing when they learn new content and decreasing when they fall behind.

2. **Fitness Score:** Measures how well a student revisits previous chapters, encouraging review of old content and considering correctness scores.

3. **Growth Score:** Serves as a progress indicator, showing students how much of the course lies ahead as they learn new concepts.

4. **Strength Score:** Encourages participation in REX-Duels, promoting interaction among students and rewarding winners with more points.

5. **Power Score:** Represents a composite value of other reward properties, enabling student ranking based on overall performance.

For more details about the Reward Service Scoring System, please refer to the [documentation](https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/Scoring%20System.html).

## API description

The GraphQL API is described in the [api.md file](api.md).

The endpoint for the GraphQL API is `/graphql`. The GraphQL Playground is available at `/graphiql`.

## How to run

How to run services locally is described in
the [wiki](https://gits-enpro.readthedocs.io/en/latest/dev-manuals/backend/get-started.html).

