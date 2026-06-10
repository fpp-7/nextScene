import React, { useEffect, useRef } from 'react';
import { View, Text, ActivityIndicator, StyleSheet, Animated } from 'react-native';
import { Clapperboard } from 'lucide-react-native';
import { colors } from '../theme/colors';

export function LoadingScreen() {
  const fadeAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(fadeAnim, {
      toValue: 1,
      duration: 600,
      useNativeDriver: true,
    }).start();
  }, []);

  return (
    <Animated.View style={[styles.container, { opacity: fadeAnim }]}>
      <View style={styles.logoIcon}>
        <Clapperboard size={32} color={colors.primaryForeground} />
      </View>
      <Text style={styles.title}>NextScene</Text>
      <ActivityIndicator size="small" color={colors.primary} style={styles.spinner} />
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoIcon: {
    width: 64,
    height: 64,
    backgroundColor: colors.primary,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 16,
  },
  title: {
    color: colors.white,
    fontSize: 28,
    fontWeight: '600',
    marginBottom: 32,
  },
  spinner: {
    marginTop: 8,
  },
});
