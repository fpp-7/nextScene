import { createBrowserRouter } from "react-router";
import { LoginPage } from "./components/login-page";
import { RegisterPage } from "./components/register-page";
import { OnboardingGenres } from "./components/onboarding-genres";
import { OnboardingColdStart } from "./components/onboarding-coldstart";
import { DiscoverPage } from "./components/discover-page";
import { RecommendationsPage } from "./components/recommendations-page";
import { MovieDetailsPage } from "./components/movie-details-page";
import { WatchlistPage } from "./components/watchlist-page";
import { ProfilePage } from "./components/profile-page";

export const router = createBrowserRouter([
  { path: "/", Component: LoginPage },
  { path: "/register", Component: RegisterPage },
  { path: "/onboarding/genres", Component: OnboardingGenres },
  { path: "/onboarding/coldstart", Component: OnboardingColdStart },
  { path: "/discover", Component: DiscoverPage },
  { path: "/recommendations", Component: RecommendationsPage },
  { path: "/movie/:id", Component: MovieDetailsPage },
  { path: "/watchlist", Component: WatchlistPage },
  { path: "/profile", Component: ProfilePage },
]);
