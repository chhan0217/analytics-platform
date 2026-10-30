import type { ComponentType } from "react";

export interface ScreenProps {
  highlightId?: string | null;
}

import SplashScreen from "./SplashScreen";
import LoginScreen from "./LoginScreen";
import OnboardingScreen from "./OnboardingScreen";
import FindIdScreen from "./FindIdScreen";
import ResetPasswordScreen from "./ResetPasswordScreen";
import RegisterTermScreen from "./RegisterTermScreen";
import RegisterPassScreen from "./RegisterPassScreen";
import RegisterInputScreen from "./RegisterInputScreen";
import RegisterCompleteScreen from "./RegisterCompleteScreen";

export const screenComponents: Record<string, ComponentType<ScreenProps>> = {
  splash: SplashScreen,
  onboard: OnboardingScreen,
  login: LoginScreen,
  find_id: FindIdScreen,
  reset_password: ResetPasswordScreen,
  register_term: RegisterTermScreen,
  register_pass: RegisterPassScreen,
  register_input: RegisterInputScreen,
  register_complete: RegisterCompleteScreen,
};
