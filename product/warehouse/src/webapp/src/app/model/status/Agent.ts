export class Agent {
    id: number;
    appName: string;
    appVersion: string;
    agentVersion: string;
    packages: string;
    excludePackages: string;
    environment: string;
    tags: string;
    methodVisibility: string;
    startedAtMillis: number;
    publishedAtMillis: number;
    pollReceivedAtMillis: number;
    nextPollExpectedAtMillis: number;
    nextPublicationExpectedAtMillis: number;
    agentAlive: boolean;
    agentLiveAndEnabled: boolean;
}