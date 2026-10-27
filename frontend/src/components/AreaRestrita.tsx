"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useSessao } from "@/lib/sessao";

export function AreaRestrita({ perfil, children }: { perfil: string; children: React.ReactNode }) {
  const { usuario, temPerfil } = useSessao();
  const router = useRouter();

  useEffect(() => {
    if (!usuario) router.replace("/");
  }, [usuario, router]);

  if (!usuario) return null;
  if (!temPerfil(perfil)) {
    return <p className="p-8 text-slate-600">Seu perfil não tem acesso a esta tela.</p>;
  }
  return <>{children}</>;
}