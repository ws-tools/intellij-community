// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy.transformations

import com.intellij.testFramework.LightProjectDescriptor
import groovy.transform.CompileStatic
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.groovy.GroovyLightProjectDescriptor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod
import org.jetbrains.plugins.groovy.lang.resolve.GroovyResolveTestCase

import static com.intellij.testFramework.PlatformTestUtil.registerExtension

@CompileStatic
class GrTransformationsTest extends GroovyResolveTestCase {

  LightProjectDescriptor projectDescriptor = GroovyLightProjectDescriptor.GROOVY_LATEST

  void 'test skip compiled classes while checking if to include synthetic members'() {
    def resolved = resolveByText('''
class MyBase {}
trait MyT {}
class MyInheritor extends Script implements MyT {
  def ppp
}
new MyInheritor().pp<caret>p
''', GrAccessorMethod)
    assert resolved.getName() == 'getPpp'
  }

  void 'test transform anonymous classes'() {
    myFixture.addFileToProject('Base.groovy', '''\
abstract class Base {
  abstract getFoo()
}
''')
    myFixture.configureByText('a.groovy', '''\
class A {
  def baz = new Base() { // no error, getFoo() exists
    def foo = 1
  }
}
''')
    myFixture.checkHighlighting()
  }

  void 'test dont transform classes while resolving annotation'() {
    fixture.addFileToProject 'foo/bar/classes.groovy', '''\
package foo.bar
class Hello {}
class World {}
'''
    registerExtension AstTransformationSupport.EP_NAME, new AstTransformationSupport() {
      @Override
      void applyTransformation(@NotNull TransformationContext context) {
        assert false: "Transformations are not allowed for $context.codeClass.name"
      }
    }, testRootDisposable

    resolveByText '''\
import static foo.bar.Hello.Foo
@Fo<caret>o
def foo() {}
''', null

    resolveByText '''\
import static foo.bar.World.*
@Fo<caret>o
def foo() {}
''', null
  }
}
